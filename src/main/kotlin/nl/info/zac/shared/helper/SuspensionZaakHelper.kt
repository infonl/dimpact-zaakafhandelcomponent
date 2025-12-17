/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.shared.helper

import jakarta.inject.Inject
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.util.time.DateTimeConverterUtil
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.Opschorting
import nl.info.client.zgw.zrc.model.generated.Verlenging
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.util.isOpgeschort
import nl.info.zac.app.zaak.ZaakRestService.Companion.AANVULLENDE_INFORMATIE_TASK_NAME
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.util.NoArgConstructor
import org.flowable.task.api.Task
import java.time.LocalDate
import java.time.Period
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

@NoArgConstructor
class SuspensionZaakHelper @Inject constructor(
    private var policyService: PolicyService,
    private var zrcClientService: ZrcClientService,
    private var zaakVariabelenService: ZaakVariabelenService,
    private val flowableTaskService: FlowableTaskService,
) {
    companion object {
        private const val SUSPENSION = "Opschorting"
        private const val RESUMING = "Hervatting"
        private const val VERLENGING = "Verlenging"
    }

    fun suspendZaak(zaak: Zaak, numberOfDays: Long, suspensionReason: String?): Zaak {
        assertPolicy(policyService.readZaakRechten(zaak).opschorten)
        assertPolicy(zaak.opschorting.reden.isNullOrEmpty())

        val zaakUUID = zaak.uuid
        val toelichting = "$SUSPENSION: $suspensionReason"
        val plannedEndDate = zaak.einddatumGepland?.plusDays(numberOfDays)
        val latestEndDateSettlement = zaak.uiterlijkeEinddatumAfdoening.plusDays(numberOfDays)
        val patchZaak = addSuspensionToZaakPatch(
            createZaakPatch(plannedEndDate, latestEndDateSettlement),
            suspensionReason,
            true
        )

        val updatedZaak = zrcClientService.patchZaak(zaakUUID, patchZaak, toelichting)
        zaakVariabelenService.setDatumtijdOpgeschort(zaakUUID, ZonedDateTime.now())
        zaakVariabelenService.setVerwachteDagenOpgeschort(zaakUUID, Math.toIntExact(numberOfDays))
        return updatedZaak
    }

    fun resumeZaak(zaak: Zaak, resumeReason: String?, resumeDate: ZonedDateTime = ZonedDateTime.now()): Zaak {
        assertPolicy(policyService.readZaakRechten(zaak).hervatten)
        assertPolicy(zaak.isOpgeschort())

        val zaakUuid = zaak.uuid
        val dateSuspended = zaakVariabelenService.findDatumtijdOpgeschort(zaakUuid)?.also {
            require(resumeDate.isAfter(it)) {
                "Resume date $resumeDate cannot be before suspension date $it"
            }
        } ?: ZonedDateTime.now()
        val expectedDaysSuspended = zaakVariabelenService.findVerwachteDagenOpgeschort(zaakUuid) ?: 0
        val daysSinceSuspended = ChronoUnit.DAYS.between(dateSuspended, resumeDate)
        val offset = daysSinceSuspended - expectedDaysSuspended
        val plannedEndDate = zaak.einddatumGepland?.plusDays(offset)
        val latestEndDateSettlement = zaak.uiterlijkeEinddatumAfdoening.plusDays(offset)

        val toelichting = "$RESUMING: $resumeReason"
        val patchZaak = addSuspensionToZaakPatch(
            createZaakPatch(plannedEndDate, latestEndDateSettlement),
            resumeReason,
            false
        )

        val updatedZaak = zrcClientService.patchZaak(zaakUuid, patchZaak, toelichting)
        zaakVariabelenService.removeDatumtijdOpgeschort(zaakUuid)
        zaakVariabelenService.removeVerwachteDagenOpgeschort(zaakUuid)
        return updatedZaak
    }

    fun extendZaakFatalDate(zaak: Zaak, numberOfDays: Long, description: String?): Zaak {
        policyService.readZaakRechten(zaak).let {
            assertPolicy(it.wijzigen)
            assertPolicy(it.verlengenDoorlooptijd)
        }

        val endDatePlanned = zaak.einddatumGepland?.plusDays(numberOfDays)
        val finalCompletionDate = zaak.uiterlijkeEinddatumAfdoening.plusDays(numberOfDays)

        return zrcClientService.patchZaak(zaak.uuid, createZaakPatch(endDatePlanned, finalCompletionDate), description)
    }

    fun extendZaak(
        zaak: Zaak,
        dueDate: LocalDate?,
        fatalDate: LocalDate,
        extensionReason: String?,
        numberOfDays: Int
    ): Zaak =
        convertToPatch(zaak, dueDate, fatalDate, extensionReason, numberOfDays).let {
            zrcClientService.patchZaak(zaak.uuid, it, "$VERLENGING: $extensionReason")
        }

    fun extendTasks(zaak: Zaak, numberOfDays: Int): List<Task> =
        flowableTaskService.listOpenTasksForZaak(zaak.uuid)
            .filter { it.dueDate != null }
            .onEach {
                it.dueDate = DateTimeConverterUtil.convertToDate(
                    DateTimeConverterUtil.convertToLocalDate(it.dueDate).plusDays(numberOfDays.toLong())
                )
                flowableTaskService.updateTask(it)
            }

    fun adjustFinalDateForOpenTasks(zaakUUID: UUID, zaakFatalDate: LocalDate): List<Task> =
        flowableTaskService.listOpenTasksForZaak(zaakUUID)
            .filter { if (it.name != null) it.name != AANVULLENDE_INFORMATIE_TASK_NAME else true }
            .filter { it.dueDate != null }
            .filter { DateTimeConverterUtil.convertToLocalDate(it.dueDate).isAfter(zaakFatalDate) }
            .onEach {
                it.dueDate = DateTimeConverterUtil.convertToDate(zaakFatalDate)
                flowableTaskService.updateTask(it)
            }

    private fun createZaakPatch(endDatePlanned: LocalDate?, finalCompletionDate: LocalDate): Zaak =
        Zaak().apply {
            if (endDatePlanned != null) {
                einddatumGepland = endDatePlanned
            }
            uiterlijkeEinddatumAfdoening = finalCompletionDate
        }

    private fun addSuspensionToZaakPatch(zaak: Zaak, reason: String?, isOpschorting: Boolean): Zaak =
        zaak.apply {
            opschorting = Opschorting().apply {
                reden = reason
                indicatie = isOpschorting
            }
        }

    fun convertToPatch(
        zaak: Zaak,
        dueDate: LocalDate?,
        fatalDate: LocalDate,
        extensionReason: String?,
        numberOfDays: Int
    ) =
        Zaak().apply {
            einddatumGepland = dueDate
            uiterlijkeEinddatumAfdoening = fatalDate
            verlenging = Verlenging().apply {
                reden = extensionReason
                // 'duur' has the ISO-8601 period format ('P(n)Y(n)M(n)D') in the ZGW ZRC API,
                // so we use [Period.toString] to convert the duration to that format
                duur = zaak.verlenging?.duur?.let { Period.ofDays(it.toInt() + numberOfDays).toString() }
                    ?: Period.ofDays(numberOfDays).toString()
            }
        }
}
