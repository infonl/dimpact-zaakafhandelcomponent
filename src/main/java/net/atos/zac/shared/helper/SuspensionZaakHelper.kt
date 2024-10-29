package net.atos.zac.shared.helper

import jakarta.inject.Inject
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.zrc.model.generated.Opschorting
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.policy.PolicyService
import org.apache.commons.lang3.StringUtils
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class SuspensionZaakHelper {
    private var policyService: PolicyService? = null
    private var zrcClientService: ZrcClientService? = null
    private var zaakVariabelenService: ZaakVariabelenService? = null

    /**
     * Default no-arg constructor, required by Weld.
     */
    constructor()

    @Inject
    internal constructor(
        policyService: PolicyService?,
        zrcClientService: ZrcClientService?,
        zaakVariabelenService: ZaakVariabelenService?
    ) {
        this.policyService = policyService
        this.zrcClientService = zrcClientService
        this.zaakVariabelenService = zaakVariabelenService
    }

    fun suspendZaak(zaak: Zaak, numberOfDays: Long, suspensionReason: String?): Zaak {
        PolicyService.assertPolicy(policyService!!.readZaakRechten(zaak).opschorten)
        PolicyService.assertPolicy(StringUtils.isEmpty(zaak.opschorting.reden))
        val zaakUUID = zaak.uuid
        val toelichting = String.format("%s: %s", SUSPENSION, suspensionReason)
        var einddatumGepland: LocalDate? = null
        if (zaak.einddatumGepland != null) {
            einddatumGepland = zaak.einddatumGepland.plusDays(numberOfDays)
        }
        val uiterlijkeEinddatumAfdoening = zaak.uiterlijkeEinddatumAfdoening.plusDays(numberOfDays)
        val patchZaak = addSuspensionToZaakPatch(
            createZaakPatch(einddatumGepland, uiterlijkeEinddatumAfdoening),
            suspensionReason,
            true
        )

        val updatedZaak = zrcClientService!!.patchZaak(zaakUUID, patchZaak, toelichting)
        zaakVariabelenService!!.setDatumtijdOpgeschort(zaakUUID, ZonedDateTime.now())
        zaakVariabelenService!!.setVerwachteDagenOpgeschort(zaakUUID, Math.toIntExact(numberOfDays))
        return updatedZaak
    }

    fun resumeZaak(zaak: Zaak, resumeReason: String?): Zaak {
        PolicyService.assertPolicy(policyService!!.readZaakRechten(zaak).hervatten)
        PolicyService.assertPolicy(zaak.isOpgeschort)
        val zaakUUID = zaak.uuid
        val datumOpgeschort =
            zaakVariabelenService!!.findDatumtijdOpgeschort(zaak.uuid).orElseGet { ZonedDateTime.now() }
        val verwachteDagenOpgeschort = zaakVariabelenService!!.findVerwachteDagenOpgeschort(zaak.uuid).orElse(0)
        val dagenVerschil = ChronoUnit.DAYS.between(datumOpgeschort, ZonedDateTime.now())
        val offset = dagenVerschil - verwachteDagenOpgeschort
        var einddatumGepland: LocalDate? = null
        if (zaak.einddatumGepland != null) {
            einddatumGepland = zaak.einddatumGepland.plusDays(offset)
        }
        val uiterlijkeEinddatumAfdoening = zaak.uiterlijkeEinddatumAfdoening.plusDays(offset)

        val toelichting = String.format("%s: %s", RESUMING, resumeReason)
        val patchZaak = addSuspensionToZaakPatch(
            createZaakPatch(einddatumGepland, uiterlijkeEinddatumAfdoening),
            resumeReason,
            false
        )

        val updatedZaak = zrcClientService!!.patchZaak(zaakUUID, patchZaak, toelichting)
        zaakVariabelenService!!.removeDatumtijdOpgeschort(zaakUUID)
        zaakVariabelenService!!.removeVerwachteDagenOpgeschort(zaakUUID)
        return updatedZaak
    }

    fun extendZaakFatalDate(zaak: Zaak, numberOfDays: Long, description: String?): Zaak {
        val zaakRechten = policyService!!.readZaakRechten(zaak)
        PolicyService.assertPolicy(zaakRechten.wijzigen && zaakRechten.verlengenDoorlooptijd)

        val zaakUUID = zaak.uuid
        var endDatePlanned: LocalDate? = null
        if (zaak.einddatumGepland != null) {
            endDatePlanned = zaak.einddatumGepland.plusDays(numberOfDays)
        }
        val finalCompletionDate = zaak.uiterlijkeEinddatumAfdoening.plusDays(numberOfDays)

        return zrcClientService!!.patchZaak(zaakUUID, createZaakPatch(endDatePlanned, finalCompletionDate), description)
    }

    private fun createZaakPatch(
        endDatePlanned: LocalDate?,
        finalCompletionDate: LocalDate
    ): Zaak {
        val zaak = Zaak()
        if (endDatePlanned != null) {
            zaak.einddatumGepland = endDatePlanned
        }
        zaak.uiterlijkeEinddatumAfdoening = finalCompletionDate
        return zaak
    }

    private fun addSuspensionToZaakPatch(
        zaak: Zaak,
        reason: String?,
        isOpschorting: Boolean
    ): Zaak {
        val opschorting = Opschorting()
        opschorting.reden = reason
        opschorting.indicatie = isOpschorting
        zaak.opschorting = opschorting
        return zaak
    }

    companion object {
        private const val SUSPENSION = "Opschorting"
        private const val RESUMING = "Hervatting"
    }
}
