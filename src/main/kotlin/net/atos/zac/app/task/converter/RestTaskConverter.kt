/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.task.converter

import jakarta.inject.Inject
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.admin.model.HumanTaskParameters
import net.atos.zac.app.formulieren.converter.RESTFormulierDefinitieConverter
import net.atos.zac.app.identity.converter.RESTGroupConverter
import net.atos.zac.app.identity.converter.RESTUserConverter
import net.atos.zac.app.policy.converter.RESTRechtenConverter
import net.atos.zac.app.task.model.RestTask
import net.atos.zac.flowable.TaakVariabelenService.readTaskData
import net.atos.zac.flowable.TaakVariabelenService.readTaskDocuments
import net.atos.zac.flowable.TaakVariabelenService.readTaskInformation
import net.atos.zac.flowable.TaakVariabelenService.readZaakIdentificatie
import net.atos.zac.flowable.TaakVariabelenService.readZaakUUID
import net.atos.zac.flowable.TaakVariabelenService.readZaaktypeOmschrijving
import net.atos.zac.flowable.TaakVariabelenService.readZaaktypeUUID
import net.atos.zac.flowable.util.TaskUtil
import net.atos.zac.formulieren.FormulierDefinitieService
import net.atos.zac.policy.PolicyService
import net.atos.zac.util.DateTimeConverterUtil
import org.flowable.identitylink.api.IdentityLinkInfo
import org.flowable.identitylink.api.IdentityLinkType
import org.flowable.task.api.TaskInfo
import java.util.UUID

@Suppress("LongParameterList")
class RestTaskConverter @Inject constructor(
    private val groepConverter: RESTGroupConverter,
    private val medewerkerConverter: RESTUserConverter,
    private val rechtenConverter: RESTRechtenConverter,
    private val policyService: PolicyService,
    private val zaakafhandelParameterService: ZaakafhandelParameterService,
    private val formulierDefinitieConverter: RESTFormulierDefinitieConverter,
    private val formulierDefinitieService: FormulierDefinitieService
) {
    fun convert(tasks: List<TaskInfo>) = tasks
        .map { convert(it) }
        .toList()

    @Suppress("LongMethod")
    fun convert(taskInfo: TaskInfo): RestTask {
        val zaaktypeOmschrijving = readZaaktypeOmschrijving(taskInfo)
        val restTaakRechten = policyService.readTaakRechten(taskInfo, zaaktypeOmschrijving).let {
            rechtenConverter.convert(it)
        }
        val restTask = RestTask(
            id = taskInfo.id,
            naam = taskInfo.name,
            status = TaskUtil.getTaakStatus(taskInfo),
            zaakUuid = readZaakUUID(taskInfo),
            zaakIdentificatie = readZaakIdentificatie(taskInfo),
            rechten = restTaakRechten,
            zaaktypeOmschrijving = if (restTaakRechten.lezen) zaaktypeOmschrijving else null,
            toelichting = if (restTaakRechten.lezen) taskInfo.description else null,
            creatiedatumTijd = if (restTaakRechten.lezen) {
                DateTimeConverterUtil.convertToZonedDateTime(taskInfo.createTime)
            } else {
                null
            },
            toekenningsdatumTijd = if (restTaakRechten.lezen) {
                DateTimeConverterUtil.convertToZonedDateTime(taskInfo.claimTime)
            } else {
                null
            },
            fataledatum = if (restTaakRechten.lezen) {
                DateTimeConverterUtil.convertToLocalDate(taskInfo.dueDate)
            } else {
                null
            },
            behandelaar = if (restTaakRechten.lezen) medewerkerConverter.convertUserId(taskInfo.assignee) else null,
            groep = if (restTaakRechten.lezen) {
                groepConverter.convertGroupId(
                    extractGroupId(taskInfo.identityLinks)
                )
            } else {
                null
            },
            taakinformatie = if (restTaakRechten.lezen) readTaskInformation(taskInfo) else null,
            taakdata = if (restTaakRechten.lezen) readTaskData(taskInfo).toMutableMap() else null,
            taakdocumenten = if (restTaakRechten.lezen) {
                readTaskDocuments(
                    taskInfo
                ).toList()
            } else {
                null
            },
            tabellen = HashMap()
        )
        if (TaskUtil.isCmmnTask(taskInfo)) {
            convertFormulierDefinitieEnReferentieTabellen(
                restTask,
                readZaaktypeUUID(taskInfo),
                taskInfo.taskDefinitionKey
            )
        } else {
            formulierDefinitieService.readFormulierDefinitie(
                taskInfo.formKey
            ).let {
                restTask.formulierDefinitie = formulierDefinitieConverter.convert(it, true, false)
            }
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
        zaakafhandelParameterService.readZaakafhandelParameters(zaaktypeUUID)
            .humanTaskParametersCollection
            .first { taskDefinitionKey == it.planItemDefinitionID }?.let {
                verwerkZaakafhandelParameters(restTask, it)
            }
    }

    private fun verwerkZaakafhandelParameters(
        restTask: RestTask,
        humanTaskParameters: HumanTaskParameters
    ) {
        restTask.formulierDefinitieId = humanTaskParameters.formulierDefinitieID
        humanTaskParameters.referentieTabellen.forEach {
            restTask.tabellen[it.veld] = it.tabel.values
                .map { value -> value.name }
                .toList()
        }
    }
}
