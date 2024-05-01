/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.taken.converter

import jakarta.inject.Inject
import net.atos.zac.app.formulieren.converter.RESTFormulierDefinitieConverter
import net.atos.zac.app.identity.converter.RESTGroupConverter
import net.atos.zac.app.identity.converter.RESTUserConverter
import net.atos.zac.app.policy.converter.RESTRechtenConverter
import net.atos.zac.app.taken.model.RESTTaak
import net.atos.zac.flowable.TaakVariabelenService
import net.atos.zac.flowable.util.TaskUtil
import net.atos.zac.formulieren.FormulierDefinitieService
import net.atos.zac.policy.PolicyService
import net.atos.zac.util.DateTimeConverterUtil
import net.atos.zac.zaaksturing.ZaakafhandelParameterService
import net.atos.zac.zaaksturing.model.HumanTaskParameters
import org.flowable.identitylink.api.IdentityLinkInfo
import org.flowable.identitylink.api.IdentityLinkType
import org.flowable.task.api.TaskInfo
import java.util.UUID

@Suppress("LongParameterList")
class RESTTaakConverter @Inject constructor(
    private val taakVariabelenService: TaakVariabelenService,
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
    fun convert(taskInfo: TaskInfo): RESTTaak {
        val zaaktypeOmschrijving = taakVariabelenService.readZaaktypeOmschrijving(taskInfo)
        val restTaakRechten = policyService.readTaakRechten(taskInfo, zaaktypeOmschrijving).let {
            rechtenConverter.convert(it)
        }
        val restTaak = RESTTaak(
            id = taskInfo.id,
            naam = taskInfo.name,
            status = TaskUtil.getTaakStatus(taskInfo),
            zaakUuid = taakVariabelenService.readZaakUUID(taskInfo),
            zaakIdentificatie = taakVariabelenService.readZaakIdentificatie(taskInfo),
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
            taakinformatie = if (restTaakRechten.lezen) taakVariabelenService.readTaakinformatie(taskInfo) else null,
            taakdata = if (restTaakRechten.lezen) taakVariabelenService.readTaakdata(taskInfo).toMutableMap() else null,
            taakdocumenten = if (restTaakRechten.lezen) {
                taakVariabelenService.readTaakdocumenten(
                    taskInfo
                ).toList()
            } else {
                null
            },
            tabellen = HashMap()
        )
        if (TaskUtil.isCmmnTask(taskInfo)) {
            convertFormulierDefinitieEnReferentieTabellen(
                restTaak,
                taakVariabelenService.readZaaktypeUUID(taskInfo),
                taskInfo.taskDefinitionKey
            )
        } else {
            formulierDefinitieService.readFormulierDefinitie(
                taskInfo.formKey
            ).let {
                restTaak.formulierDefinitie = formulierDefinitieConverter.convert(it, true, false)
            }
        }
        return restTaak
    }

    fun extractGroupId(identityLinks: List<IdentityLinkInfo>): String? =
        identityLinks.firstOrNull { IdentityLinkType.CANDIDATE == it.type }?.groupId

    private fun convertFormulierDefinitieEnReferentieTabellen(
        restTaak: RESTTaak,
        zaaktypeUUID: UUID,
        taskDefinitionKey: String
    ) {
        zaakafhandelParameterService.readZaakafhandelParameters(zaaktypeUUID)
            .humanTaskParametersCollection
            .first { taskDefinitionKey == it.planItemDefinitionID }?.let {
                verwerkZaakafhandelParameters(restTaak, it)
            }
    }

    private fun verwerkZaakafhandelParameters(
        restTaak: RESTTaak,
        humanTaskParameters: HumanTaskParameters
    ) {
        restTaak.formulierDefinitieId = humanTaskParameters.formulierDefinitieID
        humanTaskParameters.referentieTabellen.forEach {
            restTaak.tabellen[it.veld] = it.tabel.waarden
                .map { value -> value.naam }
                .toList()
        }
    }
}
