/*
 *  SPDX-FileCopyrightText: 2025 INFO.nl
 *  SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.search.model

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.app.policy.model.RestDocumentRechten
import nl.info.zac.app.policy.model.toRestDocumentRechten
import nl.info.zac.policy.output.DocumentRechten
import nl.info.zac.search.model.DocumentIndicatie
import nl.info.zac.search.model.zoekobject.DocumentZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.util.time.toLocalDate
import java.time.LocalDate
import java.util.EnumSet

data class RestDocumentZoekObject(
    override val id: String? = null,
    override val type: ZoekObjectType? = null,
    override val identificatie: String? = null,
    val titel: String? = null,
    val beschrijving: String? = null,
    val zaaktypeUuid: String? = null,
    val zaaktypeIdentificatie: String? = null,
    val zaaktypeOmschrijving: String? = null,
    val zaakIdentificatie: String? = null,
    val zaakUuid: String? = null,
    val zaakRelatie: String? = null,

    @get:JsonbProperty("isZaakspecifiekGeautoriseerd")
    val isZaakspecifiekGeautoriseerd: Boolean = false,

    val creatiedatum: LocalDate? = null,
    val registratiedatum: LocalDate? = null,
    val ontvangstdatum: LocalDate? = null,
    val verzenddatum: LocalDate? = null,
    val ondertekeningDatum: LocalDate? = null,
    val vertrouwelijkheidaanduiding: String? = null,
    val auteur: String? = null,
    val status: String? = null,
    val formaat: String? = null,
    val versie: Long = 0,
    val bestandsnaam: String? = null,
    val bestandsomvang: Long = 0,
    val documentType: String? = null,
    val ondertekeningSoort: String? = null,

    @get:JsonbProperty("isOndertekend")
    val isOndertekend: Boolean = false,

    val inhoudUrl: String? = null,

    @get:JsonbProperty("isVergrendeld")
    val isVergrendeld: Boolean = false,

    val vergrendeldDoor: String? = null,
    val indicaties: EnumSet<DocumentIndicatie>? = null,
    val rechten: RestDocumentRechten? = null,
    val hasGebruiksrecht: Boolean = false
) : AbstractRestZoekObject(id, type, identificatie)

fun DocumentZoekObject.toRestDocumentZoekObject(documentRechten: DocumentRechten) = RestDocumentZoekObject(
    id = this@toRestDocumentZoekObject.getObjectId(),
    type = this@toRestDocumentZoekObject.getType(),
    titel = this@toRestDocumentZoekObject.titel,
    beschrijving = this@toRestDocumentZoekObject.beschrijving,
    zaaktypeUuid = this@toRestDocumentZoekObject.zaaktypeUuid,
    zaaktypeIdentificatie = this@toRestDocumentZoekObject.zaaktypeIdentificatie,
    zaaktypeOmschrijving = this@toRestDocumentZoekObject.zaaktypeOmschrijving,
    zaakIdentificatie = this@toRestDocumentZoekObject.zaakIdentificatie,
    zaakUuid = this@toRestDocumentZoekObject.zaakUuid,
    zaakRelatie = this@toRestDocumentZoekObject.zaakRelatie,
    isZaakspecifiekGeautoriseerd = this@toRestDocumentZoekObject.isZaakspecifiekGeautoriseerd,
    creatiedatum = this@toRestDocumentZoekObject.creatiedatum?.toLocalDate(),
    registratiedatum = this@toRestDocumentZoekObject.registratiedatum?.toLocalDate(),
    ontvangstdatum = this@toRestDocumentZoekObject.ontvangstdatum?.toLocalDate(),
    verzenddatum = this@toRestDocumentZoekObject.verzenddatum?.toLocalDate(),
    ondertekeningDatum = this@toRestDocumentZoekObject.ondertekeningDatum?.toLocalDate(),
    vertrouwelijkheidaanduiding = this@toRestDocumentZoekObject.vertrouwelijkheidaanduiding,
    auteur = this@toRestDocumentZoekObject.auteur,
    status = this@toRestDocumentZoekObject.getStatus()?.toString(),
    formaat = this@toRestDocumentZoekObject.formaat,
    versie = this@toRestDocumentZoekObject.versie,
    bestandsnaam = this@toRestDocumentZoekObject.bestandsnaam,
    bestandsomvang = this@toRestDocumentZoekObject.bestandsomvang,
    documentType = this@toRestDocumentZoekObject.documentType,
    ondertekeningSoort = this@toRestDocumentZoekObject.ondertekeningSoort,
    isOndertekend = this@toRestDocumentZoekObject.isIndicatie(DocumentIndicatie.ONDERTEKEND),
    inhoudUrl = this@toRestDocumentZoekObject.inhoudUrl,
    isVergrendeld = this@toRestDocumentZoekObject.isIndicatie(DocumentIndicatie.VERGRENDELD),
    vergrendeldDoor = this@toRestDocumentZoekObject.vergrendeldDoorNaam,
    indicaties = this@toRestDocumentZoekObject.getDocumentIndicaties()
        .filter { it != DocumentIndicatie.GEBRUIKSRECHT }
        .toCollection(EnumSet.noneOf(DocumentIndicatie::class.java)),
    rechten = documentRechten.toRestDocumentRechten()
)
