/*
 * SPDX-FileCopyrightText: 2021 Dimpact, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.task.converter

import jakarta.inject.Inject
import nl.info.zac.flowable.task.readTaskData
import nl.info.zac.flowable.task.readTaskDocuments
import nl.info.zac.flowable.task.readTaskInformation
import nl.info.zac.flowable.task.readZaakIdentificatie
import nl.info.zac.flowable.task.readZaakUUID
import nl.info.zac.flowable.task.readZaaktypeOmschrijving
import nl.info.zac.flowable.task.readZaaktypeUUID
import nl.info.zac.flowable.util.isCmmnTask
import nl.info.zac.flowable.util.taakStatus
import nl.info.zac.util.time.convertToLocalDate
import nl.info.zac.util.time.convertToZonedDateTime
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.admin.model.ZaaktypeCmmnHumantaskParameters
import nl.info.zac.app.identity.converter.RestGroupConverter
import nl.info.zac.app.identity.converter.RestUserConverter
import nl.info.zac.app.policy.model.toRestTaakRechten
import nl.info.zac.app.task.model.RestTask
import nl.info.zac.flowable.bpmn.BpmnProcessDefinitionTaskFormService
import nl.info.zac.policy.PolicyService
import nl.info.zac.search.model.ZaakAutorisatieGegevens
import org.flowable.identitylink.api.IdentityLinkInfo
import org.flowable.identitylink.api.IdentityLinkType
import org.flowable.task.api.TaskInfo
import java.util.UUID

@Suppress("LongParameterList")
class RestTaskConverter @Inject constructor(
    private val groepConverter: RestGroupConverter,
    private val medewerkerConverter: RestUserConverter,
    private val policyService: PolicyService,
    private val zaaktypeConfigurationService: ZaaktypeConfigurationService,
    private val bpmnProcessDefinitionTaskFormService: BpmnProcessDefinitionTaskFormService,
) {
    fun toRestTasks(tasks: List<TaskInfo>) = tasks.map { toRestTask(it) }


    fun toRestTasks(tasks: List<TaskInfo>, zaakAutorisatieGegevens: ZaakAutorisatieGegevens) =
        tasks.map { toRestTask(it, zaakAutorisatieGegevens) }

    @Suppress("LongMethod", "ComplexMethod")
    fun toRestTask(
        taskInfo: TaskInfo,
        zaakAutorisatieGegevens: ZaakAutorisatieGegevens? = null
    ): RestTask {
        val zaaktypeOmschrijving = readZaaktypeOmschrijving(taskInfo)
        val restTaakRechten = policyService.readTaakRechten(
            taskInfo = taskInfo,
            zaaktypeOmschrijving = zaaktypeOmschrijving,
            zaakAutorisatieGegevens = zaakAutorisatieGegevens
        ).toRestTaakRechten()
        val restTask = RestTask(
            id = taskInfo.id,
            naam = taskInfo.name,
            status = taskInfo.taakStatus(),
            zaakUuid = readZaakUUID(taskInfo),
            zaakIdentificatie = readZaakIdentificatie(taskInfo),
            rechten = restTaakRechten,
            zaaktypeOmschrijving = if (restTaakRechten.canLezen) zaaktypeOmschrijving else null,
            zaaktypeUUID = readZaaktypeUUID(taskInfo),
            toelichting = if (restTaakRechten.canLezen) taskInfo.description else null,
            creatiedatumTijd = if (restTaakRechten.canLezen) {
                taskInfo.createTime?.let(::convertToZonedDateTime)
            } else {
                null
            },
            toekenningsdatumTijd = if (restTaakRechten.canLezen) {
                taskInfo.claimTime?.let(::convertToZonedDateTime)
            } else {
                null
            },
            fataledatum = if (restTaakRechten.canLezen) {
                taskInfo.dueDate?.let(::convertToLocalDate)
            } else {
                null
            },
            behandelaar = if (restTaakRechten.canLezen) {
                taskInfo.assignee?.let {
                    medewerkerConverter.convertUserId(
                        it
                    )
                }
            } else {
                null
            },
            groep = if (restTaakRechten.canLezen) {
                extractGroupId(taskInfo.identityLinks)?.let { groepConverter.convertGroupId(it) }
            } else {
                null
            },
            taakinformatie = if (restTaakRechten.canLezen) readTaskInformation(taskInfo) else null,
            taakdata = if (restTaakRechten.canLezen) readTaskData(taskInfo).toMutableMap() else null,
            taakdocumenten = if (restTaakRechten.canLezen) {
                readTaskDocuments(
                    taskInfo
                )
            } else {
                null
            },
            tabellen = HashMap()
        )
        if (taskInfo.isCmmnTask()) {
            convertFormulierDefinitieEnReferentieTabellen(
                restTask,
                readZaaktypeUUID(taskInfo),
                taskInfo.taskDefinitionKey
            )
        } else {
            restTask.formioFormulier = bpmnProcessDefinitionTaskFormService.readForm(
                taskInfo.processDefinitionId,
                taskInfo.formKey
            )
        }
        return restTask
    }

    fun extractGroupId(identityLinks: List<IdentityLinkInfo>): String? =
        identityLinks.firstOrNull { IdentityLinkType.CANDIDATE == it.type }?.groupId

    private fun convertFormulierDefinitieEnReferentieTabellen(
        restTask: RestTask,
        zaaktypeUUID: UUID,
        taskDefinitionKey: String
    ) {
        zaaktypeConfigurationService.findConfiguration(zaaktypeUUID)
            ?.cmmnExtension
            ?.getHumanTaskParametersCollection()
            .orEmpty()
            .first { taskDefinitionKey == it.planItemDefinitionID }.let {
                verwerkZaakafhandelParameters(restTask, it)
            }
    }

    private fun verwerkZaakafhandelParameters(
        restTask: RestTask,
        zaaktypeCmmnHumantaskParameters: ZaaktypeCmmnHumantaskParameters
    ) {
        restTask.formulierDefinitieId = zaaktypeCmmnHumantaskParameters.getFormulierDefinitieID()
        zaaktypeCmmnHumantaskParameters.getReferentieTabellen().forEach { humanTaskReferentieTabel ->
            val veld = humanTaskReferentieTabel.veld ?: return@forEach
            val tabel = humanTaskReferentieTabel.tabel ?: return@forEach
            restTask.tabellen[veld] = tabel.values.map { it.name }
        }
    }
}
