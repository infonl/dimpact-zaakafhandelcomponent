/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.smartdocuments.model.document

import jakarta.json.bind.annotation.JsonbDateFormat
import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.documentcreation.converter.DocumentCreationDataConverter.Companion.DATE_FORMAT
import java.time.LocalDate

data class AanvragerData(
    val naam: String? = null,
    val straat: String? = null,
    val huisnummer: String? = null,
    val postcode: String? = null,
    val woonplaats: String? = null
)

data class Data(
    @field:JsonbProperty("aanvrager")
    val aanvragerData: AanvragerData? = null,

    @field:JsonbProperty("gebruiker")
    val gebruikerData: GebruikerData,

    @field:JsonbProperty("startformulier")
    val startformulierData: StartformulierData? = null,

    @field:JsonbProperty("taak")
    val taskData: TaskData? = null,

    @field:JsonbProperty("zaak")
    val zaakData: ZaakData
)

data class Deposit(
    @field:JsonbProperty("SmartDocument")
    val smartDocument: SmartDocument,

    @field:JsonbProperty("data")
    val data: Data? = null
)

data class GebruikerData(
    val id: String,
    val naam: String
)

data class OutputFormat(
    @field:JsonbProperty("OutputFormat")
    val outputFormat: String
)

data class Selection(
    @field:JsonbProperty("TemplateGroup")
    val templateGroup: String? = null,

    @field:JsonbProperty("Template")
    val template: String? = null,

    @field:JsonbProperty("FixedValues")
    val fixedValues: String = ""
)

data class SmartDocument(
    @field:JsonbProperty("Selection")
    val selection: Selection,

    @field:JsonbProperty("Variables")
    val variables: Variables? = null
)

data class StartformulierData(
    val productAanvraagtype: String,

    val data: Map<String, Any>
)

data class TaskData(
    val naam: String,
    var behandelaar: String? = null
)

data class Variables(
    @field:JsonbProperty("OutputFormats")
    val outputFormats: List<OutputFormat>,

    @field:JsonbProperty("RedirectUrl")
    val redirectUrl: String,

    @field:JsonbProperty("RedirectMethod")
    val redirectMethod: String
)

data class ZaakData(
    val behandelaar: String? = null,

    val besluit: String? = null,

    val communicatiekanaal: String? = null,

    @field:JsonbDateFormat(DATE_FORMAT)
    val einddatum: LocalDate? = null,

    @field:JsonbDateFormat(DATE_FORMAT)
    val einddatumGepland: LocalDate? = null,

    val groep: String? = null,

    val identificatie: String? = null,

    val omschrijving: String? = null,

    val opschortingReden: String? = null,

    @field:JsonbDateFormat(DATE_FORMAT)
    val registratiedatum: LocalDate? = null,

    val resultaat: String? = null,

    @field:JsonbDateFormat(DATE_FORMAT)
    val startdatum: LocalDate? = null,

    val status: String? = null,

    val toelichting: String? = null,
    @field:JsonbDateFormat(DATE_FORMAT)
    val uiterlijkeEinddatumAfdoening: LocalDate? = null,

    val vertrouwelijkheidaanduiding: String? = null,

    val verlengingReden: String? = null,

    val zaaktype: String? = null
)
