/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.task.converter

import jakarta.inject.Inject
import jakarta.json.bind.annotation.JsonbDateFormat
import net.atos.zac.app.task.model.RestTaskHistoryLine
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.flowable.task.model.ValueChangeData
import net.atos.zac.identity.IdentityService
import net.atos.zac.identity.model.getFullName
import net.atos.zac.util.JsonbUtil
import net.atos.zac.util.time.DateTimeConverterUtil
import org.flowable.task.api.history.HistoricTaskLogEntry
import org.flowable.task.api.history.HistoricTaskLogEntryType
import java.util.Date

class RestTaskHistoryConverter @Inject constructor(
    private val identityService: IdentityService
) {
    companion object {
        const val CREATED_ATTRIBUUT_LABEL = "aangemaakt"
        const val COMPLETED_ATTRIBUUT_LABEL = "afgerond"
        const val GROEP_ATTRIBUUT_LABEL = "groep"
        const val BEHANDELAAR_ATTRIBUUT_LABEL = "behandelaar"
        const val TOELICHTING_ATTRIBUUT_LABEL = "toelichting"
        const val AANGEMAAKT_DOOR_ATTRIBUUT_LABEL = "aangemaaktDoor"
        const val FATALEDATUM_ATTRIBUUT_LABEL = "fataledatum"
        const val STATUS_ATTRIBUUT_LABEL = "taak.status"
    }

    fun convert(historicTaskLogEntries: List<HistoricTaskLogEntry>): List<RestTaskHistoryLine> =
        historicTaskLogEntries
            .map { convert(it) }
            .mapNotNull { it }
            .toList()

    private fun convert(historicTaskLogEntry: HistoricTaskLogEntry): RestTaskHistoryLine? {
        val restTaakHistorieRegel = when (historicTaskLogEntry.type) {
            FlowableTaskService.USER_TASK_DESCRIPTION_CHANGED -> convertValueChangeData(
                TOELICHTING_ATTRIBUUT_LABEL,
                historicTaskLogEntry.data
            )

            FlowableTaskService.USER_TASK_ASSIGNEE_CHANGED_CUSTOM -> convertValueChangeData(
                BEHANDELAAR_ATTRIBUUT_LABEL,
                historicTaskLogEntry.data
            )
            FlowableTaskService.USER_TASK_GROUP_CHANGED -> convertValueChangeData(
                GROEP_ATTRIBUUT_LABEL,
                historicTaskLogEntry.data
            )
            else -> historicTaskLogEntry.data?.let {
                convertData(
                    HistoricTaskLogEntryType.valueOf(historicTaskLogEntry.type),
                    it
                )
            }
        }
        restTaakHistorieRegel?.datumTijd = DateTimeConverterUtil.convertToZonedDateTime(historicTaskLogEntry.timeStamp)
        return restTaakHistorieRegel
    }

    private fun convertData(historicTaskLogEntryType: HistoricTaskLogEntryType, data: String): RestTaskHistoryLine? =
        when (historicTaskLogEntryType) {
            HistoricTaskLogEntryType.USER_TASK_CREATED -> RestTaskHistoryLine(
                STATUS_ATTRIBUUT_LABEL,
                null,
                CREATED_ATTRIBUUT_LABEL,
                null
            )
            HistoricTaskLogEntryType.USER_TASK_COMPLETED -> RestTaskHistoryLine(
                STATUS_ATTRIBUUT_LABEL,
                CREATED_ATTRIBUUT_LABEL,
                COMPLETED_ATTRIBUUT_LABEL,
                null
            )
            HistoricTaskLogEntryType.USER_TASK_OWNER_CHANGED -> convertOwnerChanged(data)
            HistoricTaskLogEntryType.USER_TASK_DUEDATE_CHANGED -> convertDuedateChanged(data)
            // unsupported types result in null return value
            else -> null
        }

    private fun convertValueChangeData(attribuutLabel: String, data: String): RestTaskHistoryLine {
        JsonbUtil.JSONB.fromJson(data, ValueChangeData::class.java).let {
            return RestTaskHistoryLine(
                attribuutLabel,
                it.oldValue,
                it.newValue,
                it.explanation
            )
        }
    }

    class AssigneeChangedData {
        var newAssigneeId: String? = null
        var previousAssigneeId: String? = null
    }

    private fun convertOwnerChanged(data: String): RestTaskHistoryLine {
        JsonbUtil.JSONB.fromJson(data, AssigneeChangedData::class.java).let {
            return RestTaskHistoryLine(
                AANGEMAAKT_DOOR_ATTRIBUUT_LABEL,
                getMedewerkerFullName(it.previousAssigneeId),
                getMedewerkerFullName(it.newAssigneeId),
                null
            )
        }
    }

    private fun getMedewerkerFullName(medewerkerId: String?): String? =
        medewerkerId?.let { identityService.readUser(it).getFullName() }

    class DuedateChangedData {
        @JsonbDateFormat(JsonbDateFormat.TIME_IN_MILLIS)
        var newDueDate: Date? = null

        @JsonbDateFormat(JsonbDateFormat.TIME_IN_MILLIS)
        var previousDueDate: Date? = null
    }

    private fun convertDuedateChanged(data: String): RestTaskHistoryLine {
        JsonbUtil.JSONB.fromJson(data, DuedateChangedData::class.java).let {
            return RestTaskHistoryLine(
                FATALEDATUM_ATTRIBUUT_LABEL,
                DateTimeConverterUtil.convertToLocalDate(it.previousDueDate),
                DateTimeConverterUtil.convertToLocalDate(it.newDueDate),
                null
            )
        }
    }
}
