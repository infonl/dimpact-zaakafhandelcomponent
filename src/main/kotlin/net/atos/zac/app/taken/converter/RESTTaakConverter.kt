/*
 * SPDX-FileCopyrightText: 2021 Atos
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
import net.atos.zac.zaaksturing.model.HumanTaskReferentieTabel
import net.atos.zac.zaaksturing.model.ReferentieTabelWaarde
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
    fun convert(tasks: List<TaskInfo>): List<RESTTaak> {
        return tasks.stream()
            .map { taskInfo: TaskInfo -> this.convert(taskInfo) }
            .toList()
    }

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
            behandelaar = if (restTaakRechten.lezen)medewerkerConverter.convertUserId(taskInfo.assignee) else null,
            groep = if (restTaakRechten.lezen) {
                groepConverter.convertGroupId(
                    extractGroupId(taskInfo.identityLinks)
                )
            } else {
                null
            },
            taakinformatie = if (restTaakRechten.lezen)taakVariabelenService.readTaakinformatie(taskInfo) else null,
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
            val formulierDefinitie = formulierDefinitieService.readFormulierDefinitie(
                taskInfo.formKey
            )
            restTaak.formulierDefinitie = formulierDefinitieConverter.convert(formulierDefinitie, true, false)
        }
        return restTaak
    }

    fun extractGroupId(identityLinks: List<IdentityLinkInfo>): String? {
        return identityLinks.stream()
            .filter { identityLinkInfo: IdentityLinkInfo -> IdentityLinkType.CANDIDATE == identityLinkInfo.type }
            .findAny()
            .map { obj: IdentityLinkInfo -> obj.groupId }
            .orElse(null)
    }

    private fun convertFormulierDefinitieEnReferentieTabellen(
        restTaak: RESTTaak,
        zaaktypeUUID: UUID,
        taskDefinitionKey: String
    ) {
        zaakafhandelParameterService.readZaakafhandelParameters(zaaktypeUUID)
            .humanTaskParametersCollection.stream()
            .filter { zaakafhandelParameters: HumanTaskParameters -> taskDefinitionKey == zaakafhandelParameters.planItemDefinitionID }
            .findAny()
            .ifPresent { zaakafhandelParameters: HumanTaskParameters ->
                verwerkZaakafhandelParameters(
                    restTaak,
                    zaakafhandelParameters
                )
            }
    }

    private fun verwerkZaakafhandelParameters(
        restTaak: RESTTaak,
        humanTaskParameters: HumanTaskParameters
    ) {
        restTaak.formulierDefinitieId = humanTaskParameters.formulierDefinitieID
        humanTaskParameters.referentieTabellen.forEach { referentieTabel: HumanTaskReferentieTabel ->
            restTaak.tabellen[referentieTabel.veld] = referentieTabel.tabel.waarden.stream()
                .map { obj: ReferentieTabelWaarde -> obj.naam }
                .toList()
        }
    }
}
