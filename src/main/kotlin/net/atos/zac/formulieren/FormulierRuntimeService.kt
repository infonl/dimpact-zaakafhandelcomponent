/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.formulieren

import jakarta.inject.Inject
import jakarta.json.Json
import jakarta.json.JsonArray
import jakarta.json.JsonObject
import jakarta.json.JsonString
import jakarta.json.JsonValue
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.zac.app.formulieren.model.FormulierData
import net.atos.zac.app.formulieren.model.RESTFormulierVeldDefinitie
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.flowable.task.TaakVariabelenService
import net.atos.zac.formulieren.model.FormulierVeldtype
import net.atos.zac.util.time.DateTimeConverterUtil
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.util.extractUuid
import nl.info.zac.admin.ReferenceTableService
import nl.info.zac.admin.model.ReferenceTableValue
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
class FormulierRuntimeService @Inject constructor(
    private val zgwApiService: ZGWApiService,
    private val zrcClientService: ZrcClientService,
    private val zaakVariabelenService: ZaakVariabelenService,
    private val identityService: IdentityService,
    private val referenceTableService: ReferenceTableService,
    private val suspensionZaakHelper: SuspensionZaakHelper,
    private val drcClientService: DrcClientService,
    private val enkelvoudigInformatieObjectUpdateService: EnkelvoudigInformatieObjectUpdateService,
    private val taakVariabelenService: TaakVariabelenService,
    private val flowableTaskService: FlowableTaskService
) {
    companion object {
        private val DATUM_FORMAAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyy")

        private const val REDEN_ZAAK_HERVATTEN = "Zaak hervat vanuit proces"

        private const val REFERENCE_TABLE_SEPARATOR = ";"

        private const val DOCUMENT_SEPARATOR = ";"

        private const val FORMIO_DEFAULT_VALUE = "defaultValue"

        private const val FORMIO_TITLE = "title"

        private const val AANTAL_DAGEN_VANAF_HEDEN_FORMAAT = "^[+-]\\d{1,4}$"

        private const val DEFAULT_VALUE_JA = "ja"
        private const val DEFAULT_VALUE_TRUE = "true"
        private const val DEFAULT_VALUE_ONE = "1"
    }

    fun renderFormulierDefinitie(restTask: RestTask) =
        restTask.formulierDefinitie?.run {
            veldDefinities.forEach { fieldDefinition ->
                fieldDefinition.defaultWaarde?.let { defaultValue ->
                    setDefaultValue(defaultValue, fieldDefinition, restTask)
                }
            }
        }

    private fun setDefaultValue(
        defaultValue: String,
        fieldDefinition: RESTFormulierVeldDefinitie,
        restTask: RestTask
    ) {
        if (defaultValue.isNotBlank()) {
            fieldDefinition.defaultWaarde = resolveDefaultValue(
                defaultValue,
                ResolveDefaultValueContext(restTask, zrcClientService, zaakVariabelenService)
            )
            fieldDefinition.veldtype?.let {
                fieldDefinition.defaultWaarde = formatDefaultValue(defaultValue, it)
            }
        }
        fieldDefinition.meerkeuzeOpties?.let {
            fieldDefinition.meerkeuzeOpties = resolveMultipleChoiceOptions(it)
        }
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
            if (formulierData.toelichting != null) {
                task.description = formulierData.toelichting
            }
            if (formulierData.taakFataleDatum != null) {
                task.dueDate = DateTimeConverterUtil.convertToDate(formulierData.taakFataleDatum)
            }
            task = flowableTaskService.updateTask(task)
        }
        if (formulierData.zaakOpschorten && !zaak.isOpgeschort) {
            suspensionZaakHelper.suspendZaak(
                zaak,
                ChronoUnit.DAYS.between(LocalDate.now(), DateTimeConverterUtil.convertToLocalDate(task.dueDate)),
                if (restTask.formulierDefinitie != null) {
                    restTask.formulierDefinitie?.naam
                } else {
                    restTask.formioFormulier?.getString(FORMIO_TITLE)
                }
            )
        }
        if (formulierData.zaakHervatten && zaak.isOpgeschort) {
            suspensionZaakHelper.resumeZaak(zaak, REDEN_ZAAK_HERVATTEN)
        }
        markDocumentAsSent(formulierData)
        markDocumentAsSigned(formulierData)

        val zaakVariablen = zaakVariabelenService.readProcessZaakdata(zaak.uuid)
        zaakVariablen.putAll(formulierData.zaakVariabelen)
        zaakVariabelenService.setZaakdata(zaak.uuid, zaakVariablen)

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
        stringJsonValueEntry: MutableMap.MutableEntry<String, JsonValue>,
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
                    context.zaakData?.getOrDefault(defaultValue.substring(1), "").toString()
                } else {
                    defaultValue
                }
        }

    private fun getGroepForZaakDefaultValue(zaak: Zaak) =
        zgwApiService.findGroepForZaak(zaak).let { group ->
            group?.betrokkeneIdentificatie?.identificatie?.let {
                identityService.readGroup(it).name
            }
        }

    private fun getBehandelaarForZaakDefaultValue(zaak: Zaak) =
        zgwApiService.findBehandelaarMedewerkerRoleForZaak(zaak).let { behandelaar ->
            behandelaar?.getIdentificatienummer()?.let {
                identityService.readUser(it).getFullName()
            }
        }

    private fun formatDefaultValue(defaultWaarde: String, veldtype: FormulierVeldtype) =
        when (veldtype) {
            FormulierVeldtype.CHECKBOX -> formatCheckboxDefaultValue(defaultWaarde)
            FormulierVeldtype.DATUM -> formatDatumDefaultValue(defaultWaarde)
            else -> defaultWaarde
        }

    private fun formatCheckboxDefaultValue(defaultWaarde: String?) =
        if (DEFAULT_VALUE_JA.equals(defaultWaarde, ignoreCase = true) ||
            DEFAULT_VALUE_TRUE.equals(defaultWaarde, ignoreCase = true) ||
            DEFAULT_VALUE_ONE.equals(defaultWaarde, ignoreCase = true)
        ) {
            true.toString()
        } else {
            false.toString()
        }

    private fun formatDatumDefaultValue(defaultWaarde: String) =
        if (defaultWaarde.matches(AANTAL_DAGEN_VANAF_HEDEN_FORMAAT.toRegex())) {
            val dagen = defaultWaarde.substring(1).toInt()
            if (defaultWaarde.startsWith("+")) {
                LocalDate.now().plusDays(dagen.toLong()).format(DATUM_FORMAAT)
            } else {
                LocalDate.now().minusDays(dagen.toLong()).format(DATUM_FORMAAT)
            }
        } else {
            defaultWaarde
        }

    private fun resolveMultipleChoiceOptions(multipleChoiceOptions: String) =
        multipleChoiceOptions.substringAfter("REF:").let { referenceTableCode ->
            if (referenceTableCode.isNotBlank()) {
                referenceTableService.readReferenceTable(referenceTableCode).let { referenceTableValue ->
                    referenceTableValue.values
                        .sortedWith(Comparator.comparingInt(ReferenceTableValue::sortOrder))
                        .joinToString(REFERENCE_TABLE_SEPARATOR) { it.name }
                }
            } else {
                multipleChoiceOptions
            }
        }

    private fun markDocumentAsSent(formulierData: FormulierData) =
        formulierData.documentenVerzenden?.let { documentsToMark ->
            documentsToMark.split(DOCUMENT_SEPARATOR.toRegex())
                .dropLastWhile { it.isEmpty() }
                .map { UUID.fromString(it) }
                .map { drcClientService.readEnkelvoudigInformatieobject(it) }
                .forEach {
                    enkelvoudigInformatieObjectUpdateService.verzendEnkelvoudigInformatieObject(
                        it.url.extractUuid(),
                        formulierData.documentenVerzendenDatum,
                        formulierData.toelichting
                    )
                }
        }

    private fun markDocumentAsSigned(formulierData: FormulierData) =
        formulierData.documentenOndertekenen?.let { documentenOndertekenen ->
            documentenOndertekenen.split(DOCUMENT_SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }
                .map { UUID.fromString(it) }
                .map { drcClientService.readEnkelvoudigInformatieobject(it) }
                .filter { it.ondertekening == null }
                .forEach {
                    enkelvoudigInformatieObjectUpdateService.ondertekenEnkelvoudigInformatieObject(
                        it.url.extractUuid()
                    )
                }
        }
}
