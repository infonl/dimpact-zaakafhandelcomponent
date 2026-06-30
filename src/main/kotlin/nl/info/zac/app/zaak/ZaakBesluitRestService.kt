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
import nl.info.zac.app.zaak.converter.RestBesluitConverter
import nl.info.zac.app.zaak.model.besluit.RestBesluit
import nl.info.zac.app.zaak.model.besluit.RestBesluitChangeData
import nl.info.zac.app.zaak.model.besluit.RestBesluitCreateData
import nl.info.zac.app.zaak.model.besluit.RestBesluitType
import nl.info.zac.app.zaak.model.besluit.RestBesluitWithdrawalData
import nl.info.zac.app.zaak.model.besluit.toRestBesluitTypes
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.besluit.BesluitService
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
    private val besluitService: BesluitService,
    private val eventingService: EventingService,
    private val loggedInUserInstance: Instance<LoggedInUser>,
    private val policyService: PolicyService,
    private val restBesluitConverter: RestBesluitConverter,
    private val zaakHistoryLineConverter: ZaakHistoryLineConverter,
    private val zaakService: ZaakService,
    private val zrcClientService: ZrcClientService,
    private val ztcClientService: ZtcClientService
) {
    @POST
    @Path("besluit")
    fun createBesluit(@Valid besluitToevoegenGegevens: RestBesluitCreateData): RestBesluit {
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(besluitToevoegenGegevens.zaakUuid)
        assertPolicy(policyService.readZaakRechten(zaak, zaakType, loggedInUserInstance.get()).vastleggenBesluit)
        assertPolicy(CollectionUtils.isNotEmpty(zaakType.besluittypen))

        return besluitService.createBesluit(zaak, besluitToevoegenGegevens).let {
            restBesluitConverter.convertToRestBesluit(it).also {
                // This event should result from a ZAAKBESLUIT CREATED notification on the ZAKEN channel
                // but open_zaak does not send that one, so emulate it here.
                eventingService.send(ScreenEventType.ZAAK_BESLUITEN.updated(zaak))
            }
        }
    }

    @PUT
    @Path("besluit/intrekken")
    fun intrekkenBesluit(@Valid restBesluitWithdrawalData: RestBesluitWithdrawalData) =
        besluitService.readBesluit(restBesluitWithdrawalData).let { besluit ->
            zrcClientService.readZaak(besluit.zaak).let { zaak ->
                assertPolicy(
                    zaak.isOpen() && policyService.readZaakRechten(zaak, loggedInUserInstance.get()).behandelen
                )

                besluitService.withdrawBesluit(besluit, restBesluitWithdrawalData.reden).let {
                    restBesluitConverter.convertToRestBesluit(it).also {
                        // This event should result from a ZAAKBESLUIT UPDATED notification on the ZAKEN channel
                        // but open_zaak does not send that one, so emulate it here.
                        eventingService.send(ScreenEventType.ZAAK_BESLUITEN.updated(zaak))
                    }
                }
            }
        }

    @GET
    @Path("besluit/zaakUuid/{zaakUuid}")
    fun listBesluitenForZaakUUID(@PathParam("zaakUuid") zaakUUID: UUID): List<RestBesluit> {
        val loggedInUser = loggedInUserInstance.get()
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(zaakUUID)
        val zaakRechten = policyService.readZaakRechten(zaak, zaakType, loggedInUser)
        assertPolicy(zaakRechten.lezen)
        return brcClientService.listBesluiten(zaak)
            .map { restBesluitConverter.convertToRestBesluit(it) }
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
    ): List<RestBesluitType> {
        assertPolicy(policyService.readWerklijstRechten().zakenTaken)
        return ztcClientService.readBesluittypen(ztcClientService.readZaaktype(zaaktypeUUID).url)
            .filter { LocalDateUtil.dateNowIsBetween(it) }
            .toRestBesluitTypes()
    }

    @PUT
    @Path("besluit")
    fun updateBesluit(@Valid restBesluitChangeData: RestBesluitChangeData) =
        brcClientService.readBesluit(restBesluitChangeData.besluitUuid).let { besluit ->
            zrcClientService.readZaak(besluit.zaak).let { zaak ->
                assertPolicy(policyService.readZaakRechten(zaak, loggedInUserInstance.get()).vastleggenBesluit)

                besluitService.updateBesluit(besluit, restBesluitChangeData).let { updatedBesluit ->
                    restBesluitConverter.convertToRestBesluit(updatedBesluit).also {
                        // This event should result from a ZAAKBESLUIT UPDATED notification on the ZAKEN channel,
                        // but Open Zaak unfortunately does not send such a notification, so we emulate it here.
                        eventingService.send(ScreenEventType.ZAAK_BESLUITEN.updated(zaak))
                    }
                }
            }
        }
}
