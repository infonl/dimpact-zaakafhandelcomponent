/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.task

import jakarta.inject.Inject
import jakarta.json.Json
import jakarta.json.JsonArray
import jakarta.json.JsonObject
import jakarta.json.JsonString
import jakarta.json.JsonValue
import net.atos.client.zgw.drc.DrcClientService
import net.atos.zac.app.formulieren.model.FormulierData
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.flowable.task.TaakVariabelenService
import net.atos.zac.util.time.DateTimeConverterUtil
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.util.isOpgeschort
import nl.info.zac.app.informatieobjecten.EnkelvoudigInformatieObjectUpdateService
import nl.info.zac.app.task.model.RestTask
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.getFullName
import nl.info.zac.shared.helper.SuspensionZaakHelper
import org.flowable.task.api.Task
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

@Suppress("LongParameterList", "TooManyFunctions")
class BpmnTaskFormRuntimeService @Inject constructor(
    private val zgwApiService: ZgwApiService,
    private val zrcClientService: ZrcClientService,
    private val zaakVariabelenService: ZaakVariabelenService,
    private val identityService: IdentityService,
    private val suspensionZaakHelper: SuspensionZaakHelper,
    private val drcClientService: DrcClientService,
    private val enkelvoudigInformatieObjectUpdateService: EnkelvoudigInformatieObjectUpdateService,
    private val taakVariabelenService: TaakVariabelenService,
    private val flowableTaskService: FlowableTaskService
) {
    companion object {
        private val DATUM_FORMAAT = DateTimeFormatter.ofPattern("dd-MM-yyyy")

        private const val REDEN_ZAAK_HERVATTEN = "Zaak hervat vanuit proces"
        private const val DOCUMENT_SEPARATOR = ";"
        private const val FORMIO_DEFAULT_VALUE = "defaultValue"
        private const val FORMIO_TITLE = "title"
    }

    fun renderFormioFormulier(restTask: RestTask) =
        restTask.formioFormulier?.let {
            copyJsonObject(it, ResolveDefaultValueContext(restTask, zrcClientService, zaakVariabelenService))
        }

    fun submit(restTask: RestTask, task: Task, zaak: Zaak): Task {
        var task = task
        taakVariabelenService.setTaskinformation(task, restTask.taakinformatie)
        taakVariabelenService.setTaskData(task, restTask.taakdata)

        val formulierData = FormulierData(restTask.taakdata ?: emptyMap())

        if (formulierData.toelichting != null || formulierData.taakFataleDatum != null) {
            formulierData.toelichting?.let {
                task.description = it
            }
            formulierData.taakFataleDatum?.let {
                task.dueDate = DateTimeConverterUtil.convertToDate(it)
            }
            task = flowableTaskService.updateTask(task)
        }
        if (formulierData.zaakOpschorten && !zaak.isOpgeschort()) {
            suspensionZaakHelper.suspendZaak(
                zaak,
                ChronoUnit.DAYS.between(LocalDate.now(), DateTimeConverterUtil.convertToLocalDate(task.dueDate)),
                restTask.formioFormulier?.getString(FORMIO_TITLE, null)
            )
        }
        if (formulierData.zaakHervatten && zaak.isOpgeschort()) {
            suspensionZaakHelper.resumeZaak(zaak, REDEN_ZAAK_HERVATTEN)
        }
        markDocumentAsSent(formulierData)
        markDocumentAsSigned(formulierData)

        zaakVariabelenService.setZaakdata(
            zaak.uuid,
            zaakVariabelenService.readProcessZaakdata(zaak.uuid) + formulierData.zaakVariabelen
        )

        return task
    }

    private fun copyJsonObject(jsonObject: JsonObject, resolveDefaultValueContext: ResolveDefaultValueContext) =
        Json.createObjectBuilder().let { objectBuilder ->
            jsonObject.entries.forEach {
                objectBuilder.add(
                    it.key,
                    copyJsonObjectValue(it, resolveDefaultValueContext)
                )
            }
            objectBuilder.build()
        }

    private fun copyJsonObjectValue(
        stringJsonValueEntry: Map.Entry<String, JsonValue>,
        resolveDefaultValueContext: ResolveDefaultValueContext
    ) =
        if (stringJsonValueEntry.value.valueType == JsonValue.ValueType.STRING &&
            stringJsonValueEntry.key == FORMIO_DEFAULT_VALUE
        ) {
            Json.createValue(
                resolveDefaultValue(
                    (stringJsonValueEntry.value as JsonString).string,
                    resolveDefaultValueContext
                )
            )
        } else {
            copyJsonValue(stringJsonValueEntry.value, resolveDefaultValueContext)
        }

    private fun copyJsonValue(
        jsonValue: JsonValue,
        resolveDefaultValueContext: ResolveDefaultValueContext
    ): JsonValue =
        when (jsonValue.valueType) {
            JsonValue.ValueType.ARRAY -> copyJsonArray(jsonValue.asJsonArray(), resolveDefaultValueContext)
            JsonValue.ValueType.OBJECT -> copyJsonObject(jsonValue.asJsonObject(), resolveDefaultValueContext)
            else -> jsonValue
        }

    private fun copyJsonArray(
        jsonArray: JsonArray,
        resolveDefaultValueContext: ResolveDefaultValueContext
    ) =
        Json.createArrayBuilder().let { arrayBuilder ->
            jsonArray.forEach { arrayBuilder.add(copyJsonValue(it, resolveDefaultValueContext)) }
            arrayBuilder.build()
        }

    private fun resolveDefaultValue(defaultValue: String, context: ResolveDefaultValueContext): String? =
        when (defaultValue) {
            "TAAK:STARTDATUM" -> context.task.creatiedatumTijd?.format(DATUM_FORMAAT)
            "TAAK:FATALE_DATUM" -> context.task.fataledatum?.format(DATUM_FORMAAT)
            "TAAK:GROEP" -> context.task.groep?.naam
            "TAAK:BEHANDELAAR" -> context.task.behandelaar?.naam
            "ZAAK:STARTDATUM" -> context.zaak.startdatum?.format(DATUM_FORMAAT)
            "ZAAK:FATALE_DATUM" -> context.zaak.uiterlijkeEinddatumAfdoening?.format(DATUM_FORMAAT)
            "ZAAK:STREEFDATUM" -> context.zaak.einddatumGepland?.format(DATUM_FORMAAT)
            "ZAAK:GROEP" -> getGroepForZaakDefaultValue(context.zaak)
            "ZAAK:BEHANDELAAR" -> getBehandelaarForZaakDefaultValue(context.zaak)
            else ->
                if (defaultValue.startsWith(":")) {
                    context.zaakData.getOrDefault(defaultValue.substring(1), "").toString()
                } else {
                    defaultValue
                }
        }

    private fun getGroepForZaakDefaultValue(zaak: Zaak) =
        zgwApiService.findGroepForZaak(zaak).let { group ->
            group?.betrokkeneIdentificatie?.identificatie?.let {
                identityService.readGroup(it).description
            }
        }

    private fun getBehandelaarForZaakDefaultValue(zaak: Zaak) =
        zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak).let { behandelaar ->
            behandelaar?.getIdentificatienummer()?.let {
                identityService.readUser(it).getFullName()
            }
        }

    private fun markDocumentAsSent(formulierData: FormulierData) =
        formulierData.documentenVerzenden?.let { documentsToMark ->
            documentsToMark.split(DOCUMENT_SEPARATOR.toRegex())
                .dropLastWhile { it.isEmpty() }
                .map(UUID::fromString)
                .map(drcClientService::readEnkelvoudigInformatieobject)
                .forEach {
                    enkelvoudigInformatieObjectUpdateService.verzendEnkelvoudigInformatieObject(
                        uuid = it.url.extractUuid(),
                        verzenddatum = formulierData.documentenVerzendenDatum,
                        toelichting = formulierData.toelichting
                    )
                }
        }

    private fun markDocumentAsSigned(formulierData: FormulierData) =
        formulierData.documentenOndertekenen?.let { documentenOndertekenen ->
            documentenOndertekenen.split(DOCUMENT_SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }
                .map(UUID::fromString)
                .map(drcClientService::readEnkelvoudigInformatieobject)
                .filter { it.ondertekening == null }
                .forEach {
                    enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(
                        it.url.extractUuid()
                    )
                }
        }
}

private class ResolveDefaultValueContext(
    val task: RestTask,
    zrcClientService: ZrcClientService,
    zaakVariabelenService: ZaakVariabelenService
) {
    val zaak = zrcClientService.readZaak(task.zaakUuid)
    val zaakData = zaakVariabelenService.readProcessZaakdata(task.zaakUuid)
}
