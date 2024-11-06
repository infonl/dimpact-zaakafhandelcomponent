package net.atos.zac.shared.helper

import jakarta.inject.Inject
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.zrc.model.generated.Opschorting
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.policy.PolicyService
import nl.lifely.zac.util.NoArgConstructor
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@NoArgConstructor
class SuspensionZaakHelper @Inject constructor(
    private var policyService: PolicyService,
    private var zrcClientService: ZrcClientService,
    private var zaakVariabelenService: ZaakVariabelenService
) {
    companion object {
        private const val SUSPENSION = "Opschorting"
        private const val RESUMING = "Hervatting"
    }

    fun suspendZaak(zaak: Zaak, numberOfDays: Long, suspensionReason: String?): Zaak {
        policyService.readZaakRechten(zaak).let {
            PolicyService.assertPolicy(it.opschorten)
            PolicyService.assertPolicy(zaak.opschorting.reden.isNullOrEmpty())
        }

        val zaakUUID = zaak.uuid
        val toelichting = "$SUSPENSION: $suspensionReason"
        val einddatumGepland = zaak.einddatumGepland?.plusDays(numberOfDays)
        val uiterlijkeEinddatumAfdoening = zaak.uiterlijkeEinddatumAfdoening.plusDays(numberOfDays)
        val patchZaak = addSuspensionToZaakPatch(
            createZaakPatch(einddatumGepland, uiterlijkeEinddatumAfdoening),
            suspensionReason,
            true
        )

        val updatedZaak = zrcClientService.patchZaak(zaakUUID, patchZaak, toelichting)
        zaakVariabelenService.setDatumtijdOpgeschort(zaakUUID, ZonedDateTime.now())
        zaakVariabelenService.setVerwachteDagenOpgeschort(zaakUUID, Math.toIntExact(numberOfDays))
        return updatedZaak
    }

    fun resumeZaak(zaak: Zaak, resumeReason: String?): Zaak {
        policyService.readZaakRechten(zaak).let {
            PolicyService.assertPolicy(it.hervatten)
            PolicyService.assertPolicy(zaak.isOpgeschort)
        }

        val zaakUUID = zaak.uuid
        val datumOpgeschort = zaakVariabelenService.findDatumtijdOpgeschort(zaak.uuid).orElseGet { ZonedDateTime.now() }
        val verwachteDagenOpgeschort = zaakVariabelenService.findVerwachteDagenOpgeschort(zaak.uuid).orElse(0)
        val dagenVerschil = ChronoUnit.DAYS.between(datumOpgeschort, ZonedDateTime.now())
        val offset = dagenVerschil - verwachteDagenOpgeschort
        val einddatumGepland = zaak.einddatumGepland?.plusDays(offset)
        val uiterlijkeEinddatumAfdoening = zaak.uiterlijkeEinddatumAfdoening.plusDays(offset)

        val toelichting = "$RESUMING: $resumeReason"
        val patchZaak = addSuspensionToZaakPatch(
            createZaakPatch(einddatumGepland, uiterlijkeEinddatumAfdoening),
            resumeReason,
            false
        )

        val updatedZaak = zrcClientService.patchZaak(zaakUUID, patchZaak, toelichting)
        zaakVariabelenService.removeDatumtijdOpgeschort(zaakUUID)
        zaakVariabelenService.removeVerwachteDagenOpgeschort(zaakUUID)
        return updatedZaak
    }

    fun extendZaakFatalDate(zaak: Zaak, numberOfDays: Long, description: String?): Zaak {
        policyService.readZaakRechten(zaak).let {
            PolicyService.assertPolicy(it.wijzigen)
            PolicyService.assertPolicy(it.verlengenDoorlooptijd)
        }

        val endDatePlanned = zaak.einddatumGepland?.plusDays(numberOfDays)
        val finalCompletionDate = zaak.uiterlijkeEinddatumAfdoening.plusDays(numberOfDays)

        return zrcClientService.patchZaak(zaak.uuid, createZaakPatch(endDatePlanned, finalCompletionDate), description)
    }

    private fun createZaakPatch(
        endDatePlanned: LocalDate?,
        finalCompletionDate: LocalDate
    ): Zaak =
        Zaak().apply {
            if (endDatePlanned != null) {
                einddatumGepland = endDatePlanned
            }
            uiterlijkeEinddatumAfdoening = finalCompletionDate
        }

    private fun addSuspensionToZaakPatch(
        zaak: Zaak,
        reason: String?,
        isOpschorting: Boolean
    ): Zaak =
        zaak.apply {
            opschorting = Opschorting().apply {
                reden = reason
                indicatie = isOpschorting
            }
        }
}
