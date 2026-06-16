/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl, 2024 Dimpact
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import net.atos.zac.event.EventingService
import net.atos.zac.util.time.LocalDateUtil
import net.atos.zac.websocket.event.ScreenEventType
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.util.isOpen
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.app.decision.DecisionService
import nl.info.zac.app.zaak.converter.RestDecisionConverter
import nl.info.zac.app.zaak.model.RestDecision
import nl.info.zac.app.zaak.model.RestDecisionChangeData
import nl.info.zac.app.zaak.model.RestDecisionCreateData
import nl.info.zac.app.zaak.model.RestDecisionType
import nl.info.zac.app.zaak.model.RestDecisionWithdrawalData
import nl.info.zac.app.zaak.model.toRestDecisionTypes
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.history.converter.ZaakHistoryLineConverter
import nl.info.zac.history.model.HistoryLine
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.zaak.ZaakService
import org.apache.commons.collections4.CollectionUtils
import java.util.UUID

@Path("zaken")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
@Suppress("LongParameterList")
@NoArgConstructor
@AllOpen
class ZaakBesluitRestService @Inject constructor(
    private val brcClientService: BrcClientService,
    private val decisionService: DecisionService,
    private val eventingService: EventingService,
    private val loggedInUserInstance: Instance<LoggedInUser>,
    private val policyService: PolicyService,
    private val restDecisionConverter: RestDecisionConverter,
    private val zaakHistoryLineConverter: ZaakHistoryLineConverter,
    private val zaakService: ZaakService,
    private val zrcClientService: ZrcClientService,
    private val ztcClientService: ZtcClientService
) {  
    @POST
    @Path("besluit")
    fun createBesluit(@Valid besluitToevoegenGegevens: RestDecisionCreateData): RestDecision {
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(besluitToevoegenGegevens.zaakUuid)
        assertPolicy(policyService.readZaakRechten(zaak, zaakType, loggedInUserInstance.get()).vastleggenBesluit)
        assertPolicy(CollectionUtils.isNotEmpty(zaakType.besluittypen))

        return decisionService.createDecision(zaak, besluitToevoegenGegevens).let {
            restDecisionConverter.convertToRestDecision(it).also {
                // This event should result from a ZAAKBESLUIT CREATED notification on the ZAKEN channel
                // but open_zaak does not send that one, so emulate it here.
                eventingService.send(ScreenEventType.ZAAK_BESLUITEN.updated(zaak))
            }
        }
    }

    @PUT
    @Path("besluit/intrekken")
    fun intrekkenBesluit(@Valid restDecisionWithdrawalData: RestDecisionWithdrawalData) =
        decisionService.readDecision(restDecisionWithdrawalData).let { besluit ->
            zrcClientService.readZaak(besluit.zaak).let { zaak ->
                assertPolicy(
                    zaak.isOpen() && policyService.readZaakRechten(zaak, loggedInUserInstance.get()).behandelen
                )

                decisionService.withdrawDecision(besluit, restDecisionWithdrawalData.reden).let {
                    restDecisionConverter.convertToRestDecision(it).also {
                        // This event should result from a ZAAKBESLUIT UPDATED notification on the ZAKEN channel
                        // but open_zaak does not send that one, so emulate it here.
                        eventingService.send(ScreenEventType.ZAAK_BESLUITEN.updated(zaak))
                    }
                }
            }
        }

    @GET
    @Path("besluit/zaakUuid/{zaakUuid}")
    fun listBesluitenForZaakUUID(@PathParam("zaakUuid") zaakUUID: UUID): List<RestDecision> {
        val loggedInUser = loggedInUserInstance.get()
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID)
        val zaakRechten = policyService.readZaakRechten(zaak, zaakType, loggedInUser)
        assertPolicy(zaakRechten.lezen)
        return zrcClientService.readZaak(zaakUUID)
            .let { brcClientService.listBesluiten(it) }
            .map { restDecisionConverter.convertToRestDecision(it) }
    }       
        
    @GET
    @Path("besluit/{uuid}/historie")
    fun listBesluitHistorie(@PathParam("uuid") besluitUuid: UUID): List<HistoryLine> {
        val besluit = brcClientService.readBesluit(besluitUuid)
        val zaak = zrcClientService.readZaak(besluit.zaak)
        val zaakType = ztcClientService.readZaaktype(zaak.zaaktype)
        assertPolicy(policyService.readZaakRechten(zaak, zaakType, loggedInUserInstance.get()).lezen)
        return brcClientService.listAuditTrail(besluitUuid).let {
            zaakHistoryLineConverter.convert(it)
        }
    }

    @GET
    @Path("besluittypes/{zaaktypeUUID}")
    fun listBesluittypes(
        @PathParam("zaaktypeUUID") zaaktypeUUID: UUID
    ): List<RestDecisionType> {
        assertPolicy(policyService.readWerklijstRechten().zakenTaken)
        return ztcClientService.readBesluittypen(ztcClientService.readZaaktype(zaaktypeUUID).url)
            .filter { LocalDateUtil.dateNowIsBetween(it) }
            .toRestDecisionTypes()
    }

    @PUT
    @Path("besluit")
    fun updateBesluit(@Valid restDecisionChangeData: RestDecisionChangeData) =
        brcClientService.readBesluit(restDecisionChangeData.besluitUuid).let { besluit ->
            zrcClientService.readZaak(besluit.zaak).let { zaak ->
                assertPolicy(policyService.readZaakRechten(zaak, loggedInUserInstance.get()).vastleggenBesluit)

                decisionService.updateDecision(besluit, restDecisionChangeData).let {
                    restDecisionConverter.convertToRestDecision(besluit).also {
                        // This event should result from a ZAAKBESLUIT UPDATED notification on the ZAKEN channel,
                        // but Open Zaak unfortunately does not send such a notification, so we emulate it here.
                        eventingService.send(ScreenEventType.ZAAK_BESLUITEN.updated(zaak))
                    }
                }
            }
        }
}
