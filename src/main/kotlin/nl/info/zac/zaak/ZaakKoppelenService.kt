/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.zaak

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.GerelateerdeZakenZaakPatch
import nl.info.client.zgw.zrc.model.NillableHoofdzaakZaakPatch
import nl.info.client.zgw.zrc.model.generated.GerelateerdeZaak
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.event.EventingService
import nl.info.zac.exception.ErrorCode
import nl.info.zac.exception.InputValidationFailedException
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.search.IndexingService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.websocket.event.ScreenEventType
import nl.info.zac.zaak.model.RelatieType
import nl.info.zac.zaak.model.ZaakNotLinkableReason
import nl.info.zac.zaak.model.canLinkNewZaakAs
import nl.info.zac.zaak.model.getDeelzaaktypenSet
import nl.info.zac.zaak.model.newZaakNotLinkableReason
import nl.info.zac.zaak.model.toZaakLinkData
import java.util.UUID

@ApplicationScoped
@NoArgConstructor
@AllOpen
class ZaakKoppelenService @Inject constructor(
    private val eventingService: EventingService,
    private val indexingService: IndexingService,
    private val loggedInUserInstance: Instance<LoggedInUser>,
    private val policyService: PolicyService,
    private val zaakService: ZaakService,
    private val zrcClientService: ZrcClientService
) {
    fun addGerelateerdeZaak(zaak: Zaak, gerelateerdeZaak: Zaak, explanation: String?) {
        zrcClientService.patchZaak(
            zaakUUID = zaak.uuid,
            zaak = GerelateerdeZakenZaakPatch(
                gerelateerdeZaken = zaak.gerelateerdeZaken.orEmpty().let { gerelateerdeZaken ->
                    if (gerelateerdeZaken.any { it.url == gerelateerdeZaak.url }) {
                        gerelateerdeZaken
                    } else {
                        gerelateerdeZaken + GerelateerdeZaak().apply { url = gerelateerdeZaak.url }
                    }
                }
            ),
            explanation = explanation
        )
    }

    fun linkHoofdzaakAndDeelzaak(hoofdzaak: Zaak, deelzaak: Zaak) {
        zrcClientService.patchZaak(
            zaakUUID = deelzaak.uuid,
            zaak = NillableHoofdzaakZaakPatch(hoofdzaak = hoofdzaak.url)
        )
        updateHoofdzaak(hoofdzaak.uuid)
    }

    /**
     * Open Zaak only sends a notification for the deelzaak when the hoofdzaak of a deelzaak changes,
     * so the hoofdzaak is reindexed and its screen event is sent here.
     */
    fun updateHoofdzaak(hoofdzaakUuid: UUID) {
        indexingService.addOrUpdateZaakOrThrow(hoofdzaakUuid, false)
        eventingService.send(ScreenEventType.ZAAK.updated(hoofdzaakUuid))
    }

    fun readZaakToLinkNewZaakTo(zaakUuid: UUID, relatieType: RelatieType, newZaaktypeUuid: UUID): Zaak {
        assertCanLinkNewZaakAs(relatieType)
        val (zaak, zaakType) = zaakService.readZaakAndZaakTypeByZaakUUID(zaakUuid)
        val zaakRechten = policyService.readZaakRechten(
            zaak = zaak,
            zaaktype = zaakType,
            loggedInUser = loggedInUserInstance.get()
        )
        return when (
            val zaakNotLinkableReason = zaak.toZaakLinkData(zaakRechten).newZaakNotLinkableReason(
                relatieType = relatieType,
                newZaaktypeUuid = newZaaktypeUuid,
                allowedDeelzaaktypes = zaakType.getDeelzaaktypenSet()
            )
        ) {
            null -> zaak
            ZaakNotLinkableReason.NOT_AUTHORISED_TO_KOPPELEN -> throw PolicyException()
            else -> throw InputValidationFailedException(
                errorCode = ErrorCode.ERROR_CODE_VALIDATION_GENERIC,
                message = "A new zaak cannot be linked to zaak '${zaak.identificatie}' as " +
                    "$relatieType: $zaakNotLinkableReason"
            )
        }
    }

    fun addLinkToNewZaak(newZaak: Zaak, zaakToLinkTo: Zaak, relatieType: RelatieType) {
        when (relatieType) {
            RelatieType.DEELZAAK -> newZaak.hoofdzaak = zaakToLinkTo.url
            else -> newZaak.gerelateerdeZaken = listOf(GerelateerdeZaak().apply { url = zaakToLinkTo.url })
        }
    }

    /**
     * Open Zaak adds a new gerelateerde zaak to the gerelateerde zaken of the zaak it links to by itself,
     * so only the hoofdzaak of a new deelzaak needs an update.
     */
    fun completeLinkOfNewZaak(zaakToLinkTo: Zaak, relatieType: RelatieType) {
        if (relatieType == RelatieType.DEELZAAK) updateHoofdzaak(zaakToLinkTo.uuid)
    }

    private fun assertCanLinkNewZaakAs(relatieType: RelatieType) {
        if (!canLinkNewZaakAs(relatieType)) {
            throw InputValidationFailedException(
                errorCode = ErrorCode.ERROR_CODE_VALIDATION_GENERIC,
                message = "RelatieType $relatieType cannot be used to link a new zaak"
            )
        }
    }
}
