/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 Lifely
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
import net.atos.client.zgw.zrc.model.RolMedewerker
import net.atos.client.zgw.zrc.model.RolOrganisatorischeEenheid
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.zac.app.formulieren.model.FormulierData
import net.atos.zac.app.formulieren.model.RESTFormulierVeldDefinitie
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.task.FlowableTaskService
import net.atos.zac.flowable.task.TaakVariabelenService
import net.atos.zac.formulieren.model.FormulierVeldtype
import net.atos.zac.util.time.DateTimeConverterUtil
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.util.extractUuid
import nl.info.zac.admin.ReferenceTableService
import nl.info.zac.admin.model.ReferenceTableValue
import nl.info.zac.app.informatieobjecten.EnkelvoudigInformatieObjectUpdateService
import nl.info.zac.app.task.model.RestTask
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.getFullName
import nl.info.zac.shared.helper.SuspensionZaakHelper
import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils
import org.flowable.task.api.Task
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Arrays
import java.util.Optional
import java.util.UUID
import java.util.function.Consumer
import java.util.function.Function
import java.util.stream.Collectors

class FormulierRuntimeService {
    @Inject
    private val zgwApiService: ZGWApiService? = null

    @Inject
    private val zrcClientService: ZrcClientService? = null

    @Inject
    private val zaakVariabelenService: ZaakVariabelenService? = null

    @Inject
    private val identityService: IdentityService? = null

    @Inject
    private val referenceTableService: ReferenceTableService? = null

    @Inject
    private val suspensionZaakHelper: SuspensionZaakHelper? = null

    @Inject
    private val drcClientService: DrcClientService? = null

    @Inject
    private val enkelvoudigInformatieObjectUpdateService: EnkelvoudigInformatieObjectUpdateService? = null

    @Inject
    private val taakVariabelenService: TaakVariabelenService? = null

    @Inject
    private val flowableTaskService: FlowableTaskService? = null

    fun renderFormulierDefinitie(restTask: RestTask) {
        val resolveDefaultValueContext = ResolveDefaultValueContext(
            restTask, zrcClientService,
            zaakVariabelenService
        )
        restTask.formulierDefinitie!!.veldDefinities.forEach(Consumer { veldDefinitie: RESTFormulierVeldDefinitie? ->
            if (StringUtils.isNotBlank(veldDefinitie!!.defaultWaarde)) {
                veldDefinitie.defaultWaarde =
                    resolveDefaultValue(veldDefinitie.defaultWaarde, resolveDefaultValueContext)
                veldDefinitie.defaultWaarde =
                    formatDefaultValue(veldDefinitie.defaultWaarde, veldDefinitie.veldtype)
            }
            veldDefinitie.meerkeuzeOpties = resolveMultipleChoiceOptions(veldDefinitie.meerkeuzeOpties)
        })
    }

    fun renderFormioFormulier(restTask: RestTask): JsonObject? {
        return copyJsonObject(
            restTask.formioFormulier!!,
            ResolveDefaultValueContext(restTask, zrcClientService, zaakVariabelenService)
        )
    }

    fun submit(restTask: RestTask, task: Task, zaak: Zaak): Task {
        var task = task
        taakVariabelenService!!.setTaskinformation(task, restTask.taakinformatie)
        taakVariabelenService.setTaskData(task, restTask.taakdata)

        val formulierData = FormulierData(restTask.taakdata)

        if (formulierData.toelichting != null || formulierData.taakFataleDatum != null) {
            if (formulierData.toelichting != null) {
                task.setDescription(formulierData.toelichting)
            }
            if (formulierData.taakFataleDatum != null) {
                task.setDueDate(DateTimeConverterUtil.convertToDate(formulierData.taakFataleDatum))
            }
            task = flowableTaskService!!.updateTask(task)
        }
        if (formulierData.zaakOpschorten && !zaak.isOpgeschort()) {
            suspensionZaakHelper!!.suspendZaak(
                zaak,
                ChronoUnit.DAYS.between(LocalDate.now(), DateTimeConverterUtil.convertToLocalDate(task.getDueDate())),
                if (restTask.formulierDefinitie != null) restTask.formulierDefinitie!!.naam else restTask.formioFormulier!!.getString(
                    FORMIO_TITLE
                )
            )
        }
        if (formulierData.zaakHervatten && zaak.isOpgeschort()) {
            suspensionZaakHelper!!.resumeZaak(zaak, REDEN_ZAAK_HERVATTEN)
        }
        markDocumentAsSent(formulierData)
        markDocumentAsSigned(formulierData)

        val zaakVariablen = zaakVariabelenService!!.readProcessZaakdata(zaak.getUuid())
        zaakVariablen.putAll(formulierData.zaakVariabelen)
        zaakVariabelenService.setZaakdata(zaak.getUuid(), zaakVariablen)

        return task
    }

    private fun copyJsonObject(
        jsonObject: JsonObject,
        resolveDefaultValueContext: ResolveDefaultValueContext
    ): JsonObject? {
        val objectBuilder = Json.createObjectBuilder()
        jsonObject.entries.forEach(Consumer { stringJsonValueEntry: MutableMap.MutableEntry<String?, JsonValue?>? ->
            objectBuilder.add(
                stringJsonValueEntry!!.key,
                copyJsonObjectValue(stringJsonValueEntry, resolveDefaultValueContext)
            )
        })
        return objectBuilder.build()
    }

    private fun copyJsonObjectValue(
        stringJsonValueEntry: MutableMap.MutableEntry<String?, JsonValue?>,
        resolveDefaultValueContext: ResolveDefaultValueContext
    ): JsonValue? {
        return if (stringJsonValueEntry.value!!.getValueType() == JsonValue.ValueType.STRING &&
            stringJsonValueEntry.key == FORMIO_DEFAULT_VALUE
        ) Json.createValue(
            resolveDefaultValue(
                (stringJsonValueEntry.value as JsonString).getString(),
                resolveDefaultValueContext
            )
        ) else copyJsonValue(stringJsonValueEntry.value!!, resolveDefaultValueContext)
    }

    private fun copyJsonValue(
        jsonValue: JsonValue,
        resolveDefaultValueContext: ResolveDefaultValueContext
    ): JsonValue? {
        return when (jsonValue.getValueType()) {
            JsonValue.ValueType.ARRAY -> copyJsonArray(jsonValue.asJsonArray(), resolveDefaultValueContext)
            JsonValue.ValueType.OBJECT -> copyJsonObject(jsonValue.asJsonObject(), resolveDefaultValueContext)
            else -> jsonValue
        }
    }

    private fun copyJsonArray(
        jsonArray: JsonArray,
        resolveDefaultValueContext: ResolveDefaultValueContext
    ): JsonArray? {
        val arrayBuilder = Json.createArrayBuilder()
        jsonArray.forEach(Consumer { jsonValue: JsonValue? ->
            arrayBuilder.add(
                copyJsonValue(
                    jsonValue!!,
                    resolveDefaultValueContext
                )
            )
        })
        return arrayBuilder.build()
    }

    private fun resolveDefaultValue(defaultValue: String, context: ResolveDefaultValueContext): String? {
        return when (defaultValue) {
            "TAAK:STARTDATUM" -> context.getTask().creatiedatumTijd!!.format(DATUM_FORMAAT)
            "TAAK:FATALE_DATUM" -> context.getTask().fataledatum!!.format(DATUM_FORMAAT)
            "TAAK:GROEP" -> if (context.getTask().groep != null) context.getTask().groep!!.naam else null
            "TAAK:BEHANDELAAR" -> if (context.getTask().behandelaar != null) context.getTask().behandelaar!!.naam else null
            "ZAAK:STARTDATUM" -> context.getZaak().getStartdatum().format(DATUM_FORMAAT)
            "ZAAK:FATALE_DATUM" -> context.getZaak().getUiterlijkeEinddatumAfdoening().format(DATUM_FORMAAT)
            "ZAAK:STREEFDATUM" -> context.getZaak().getEinddatumGepland().format(DATUM_FORMAAT)
            "ZAAK:GROEP" -> getGroepForZaakDefaultValue(context.getZaak())
            "ZAAK:BEHANDELAAR" -> getBehandelaarForZaakDefaultValue(context.getZaak())
            else -> if (defaultValue.startsWith(":")) context.getZaakData().getOrDefault(defaultValue.substring(1), "")
                .toString() else defaultValue
        }
    }

    private fun getGroepForZaakDefaultValue(zaak: Zaak): String? {
        return Optional.ofNullable<RolOrganisatorischeEenheid?>(zgwApiService!!.findGroepForZaak(zaak))
            .map<String?>(Function { groep: RolOrganisatorischeEenheid? ->
                identityService!!.readGroup(
                    groep!!.getBetrokkeneIdentificatie().getIdentificatie()
                ).name
            })
            .orElse(null)
    }

    private fun getBehandelaarForZaakDefaultValue(zaak: Zaak): String? {
        return Optional.ofNullable<RolMedewerker?>(zgwApiService!!.findBehandelaarMedewerkerRoleForZaak(zaak))
            .map<String?>(Function { behandelaar: RolMedewerker? ->
                identityService!!.readUser(behandelaar!!.getIdentificatienummer()).getFullName()
            })
            .orElse(null)
    }

    private fun formatDefaultValue(defaultWaarde: String, veldtype: FormulierVeldtype): String? {
        return when (veldtype) {
            FormulierVeldtype.CHECKBOX -> formatCheckboxDefaultValue(defaultWaarde)
            FormulierVeldtype.DATUM -> formatDatumDefaultValue(defaultWaarde)
            else -> defaultWaarde
        }
    }

    private fun formatCheckboxDefaultValue(defaultWaarde: String?): String {
        return if (StringUtils.equalsIgnoreCase("ja", defaultWaarde) ||
            StringUtils.equalsIgnoreCase("true", defaultWaarde) ||
            StringUtils.equals("1", defaultWaarde)
        ) BooleanUtils.TRUE else BooleanUtils.FALSE
    }

    private fun formatDatumDefaultValue(defaultWaarde: String): String {
        if (defaultWaarde.matches(AANTAL_DAGEN_VANAF_HEDEN_FORMAAT.toRegex())) {
            val dagen = StringUtils.substring(defaultWaarde, 1).toInt()
            if (defaultWaarde.startsWith("+")) {
                return LocalDate.now().plusDays(dagen.toLong()).format(DATUM_FORMAAT)
            } else {
                return LocalDate.now().minusDays(dagen.toLong()).format(DATUM_FORMAAT)
            }
        } else {
            return defaultWaarde
        }
    }

    private fun resolveMultipleChoiceOptions(meerkeuzeOpties: String?): String? {
        val referenceTableCode = StringUtils.substringAfter(meerkeuzeOpties, "REF:")
        if (StringUtils.isNotBlank(referenceTableCode)) {
            val referenceTable = referenceTableService!!.readReferenceTable(referenceTableCode!!)
            return referenceTable.values
                .stream()
                .sorted(Comparator.comparingInt<ReferenceTableValue>(ReferenceTableValue::sortOrder))
                .map<String?>(ReferenceTableValue::name)
                .collect(Collectors.joining(REFERENCE_TABLE_SEPARATOR))
        } else {
            return meerkeuzeOpties
        }
    }

    private fun markDocumentAsSent(formulierData: FormulierData) {
        if (formulierData.documentenVerzenden != null) {
            Arrays.stream<String>(
                formulierData.documentenVerzenden.split(DOCUMENT_SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray())
                .map<UUID?> { name: String? -> UUID.fromString(name) }
                .map<EnkelvoudigInformatieObject?> { uuid: UUID? ->
                    drcClientService!!.readEnkelvoudigInformatieobject(
                        uuid
                    )
                }
                .forEach { enkelvoudigInformatieObject: EnkelvoudigInformatieObject? ->
                    enkelvoudigInformatieObjectUpdateService!!.verzendEnkelvoudigInformatieObject(
                        enkelvoudigInformatieObject!!.getUrl().extractUuid(),
                        formulierData.documentenVerzendenDatum,
                        formulierData.toelichting
                    )
                }
        }
    }

    private fun markDocumentAsSigned(formulierData: FormulierData) {
        if (formulierData.documentenOndertekenen != null) {
            Arrays.stream<String>(
                formulierData.documentenOndertekenen.split(DOCUMENT_SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray())
                .map<UUID?> { name: String? -> UUID.fromString(name) }
                .map<EnkelvoudigInformatieObject?> { uuid: UUID? ->
                    drcClientService!!.readEnkelvoudigInformatieobject(
                        uuid
                    )
                }
                .filter { enkelvoudigInformatieobject: EnkelvoudigInformatieObject? -> enkelvoudigInformatieobject!!.getOndertekening() == null }
                .forEach { enkelvoudigInformatieobject: EnkelvoudigInformatieObject? ->
                    enkelvoudigInformatieObjectUpdateService!!.ondertekenEnkelvoudigInformatieObject(
                        enkelvoudigInformatieobject!!.getUrl().extractUuid()
                    )
                }
        }
    }

    companion object {
        private val DATUM_FORMAAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyy")

        private const val REDEN_ZAAK_HERVATTEN = "Zaak hervat vanuit proces"

        private const val REFERENCE_TABLE_SEPARATOR = ";"

        private const val DOCUMENT_SEPARATOR = ";"

        private const val FORMIO_DEFAULT_VALUE = "defaultValue"

        private const val FORMIO_TITLE = "title"

        private const val AANTAL_DAGEN_VANAF_HEDEN_FORMAAT = "^[+-]\\d{1,4}$"
    }
}
