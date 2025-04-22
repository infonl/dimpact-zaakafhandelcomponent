/*
 * SPDX-FileCopyrightText: 2021 Dimpact, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.task.converter

import jakarta.inject.Inject
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.admin.model.HumanTaskParameters
import net.atos.zac.app.formulieren.converter.toRESTFormulierDefinitie
import net.atos.zac.app.policy.converter.RestRechtenConverter
import net.atos.zac.flowable.task.TaakVariabelenService.readTaskData
import net.atos.zac.flowable.task.TaakVariabelenService.readTaskDocuments
import net.atos.zac.flowable.task.TaakVariabelenService.readTaskInformation
import net.atos.zac.flowable.task.TaakVariabelenService.readZaakIdentificatie
import net.atos.zac.flowable.task.TaakVariabelenService.readZaakUUID
import net.atos.zac.flowable.task.TaakVariabelenService.readZaaktypeOmschrijving
import net.atos.zac.flowable.task.TaakVariabelenService.readZaaktypeUUID
import net.atos.zac.flowable.util.TaskUtil
import net.atos.zac.formulieren.FormulierDefinitieService
import net.atos.zac.policy.PolicyService
import net.atos.zac.util.time.DateTimeConverterUtil
import nl.info.zac.app.identity.converter.RestGroupConverter
import nl.info.zac.app.identity.converter.RestUserConverter
import nl.info.zac.app.task.model.RestTask
import nl.info.zac.formio.FormioService
import org.flowable.identitylink.api.IdentityLinkInfo
import org.flowable.identitylink.api.IdentityLinkType
import org.flowable.task.api.TaskInfo
import java.util.UUID

@Suppress("LongParameterList")
class RestTaskConverter @Inject constructor(
    private val groepConverter: RestGroupConverter,
    private val medewerkerConverter: RestUserConverter,
    private val policyService: PolicyService,
    private val zaakafhandelParameterService: ZaakafhandelParameterService,
    private val formulierDefinitieService: FormulierDefinitieService,
    private val formioService: FormioService,
) {
    fun convert(tasks: List<TaskInfo>) = tasks.map(::convert)

    @Suppress("LongMethod", "ComplexMethod")
    fun convert(taskInfo: TaskInfo): RestTask {
        val zaaktypeOmschrijving = readZaaktypeOmschrijving(taskInfo)
        val restTaakRechten = policyService.readTaakRechten(taskInfo, zaaktypeOmschrijving).let {
            RestRechtenConverter.convert(it)
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
            behandelaar = if (restTaakRechten.lezen) {
                taskInfo.assignee?.let {
                    medewerkerConverter.convertUserId(
                        it
                    )
                }
            } else {
                null
            },
            groep = if (restTaakRechten.lezen) {
                extractGroupId(taskInfo.identityLinks)?.let { groepConverter.convertGroupId(it) }
            } else {
                null
            },
            taakinformatie = if (restTaakRechten.lezen) readTaskInformation(taskInfo) else null,
            taakdata = if (restTaakRechten.lezen) readTaskData(taskInfo).toMutableMap() else null,
            taakdocumenten = if (restTaakRechten.lezen) {
                readTaskDocuments(
                    taskInfo
                )
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
            formulierDefinitieService.findFormulierDefinitie(taskInfo.formKey).let {
                if (it != null) {
                    restTask.formulierDefinitie = it.toRESTFormulierDefinitie(true)
                } else {
                    restTask.formioFormulier = formioService.readFormioFormulier(taskInfo.formKey)
                }
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
        humanTaskParameters.referentieTabellen.forEach { humanTaskReferentieTabel ->
            restTask.tabellen[humanTaskReferentieTabel.veld] = humanTaskReferentieTabel.tabel.values
                .map { it.name }
        }
    }
}
