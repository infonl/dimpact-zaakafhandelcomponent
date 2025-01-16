/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zoeken.model

import net.atos.zac.app.policy.converter.RestRechtenConverter
import net.atos.zac.app.policy.model.RestDocumentRechten
import net.atos.zac.policy.output.DocumentRechten
import net.atos.zac.util.time.DateTimeConverterUtil.convertToLocalDate
import net.atos.zac.zoeken.model.DocumentIndicatie
import net.atos.zac.zoeken.model.zoekobject.DocumentZoekObject
import net.atos.zac.zoeken.model.zoekobject.ZoekObjectType
import nl.info.zac.util.NoArgConstructor
import java.time.LocalDate
import java.util.EnumSet

@NoArgConstructor
data class RestDocumentZoekObject(
    override var id: String? = null,
    override var type: ZoekObjectType? = null,
    override var identificatie: String? = null,
    var titel: String? = null,
    var beschrijving: String? = null,
    var zaaktypeUuid: String? = null,
    var zaaktypeIdentificatie: String? = null,
    var zaaktypeOmschrijving: String? = null,
    var zaakIdentificatie: String? = null,
    var zaakUuid: String? = null,
    var zaakRelatie: String? = null,
    var creatiedatum: LocalDate? = null,
    var registratiedatum: LocalDate? = null,
    var ontvangstdatum: LocalDate? = null,
    var verzenddatum: LocalDate? = null,
    var ondertekeningDatum: LocalDate? = null,
    var vertrouwelijkheidaanduiding: String? = null,
    var auteur: String? = null,
    var status: String? = null,
    var formaat: String? = null,
    var versie: Long = 0,
    var bestandsnaam: String? = null,
    var bestandsomvang: Long = 0,
    var documentType: String? = null,
    var ondertekeningSoort: String? = null,
    var indicatieOndertekend: Boolean = false,
    var inhoudUrl: String? = null,
    var indicatieVergrendeld: Boolean = false,
    var vergrendeldDoor: String? = null,
    var indicaties: EnumSet<DocumentIndicatie>? = null,
    var rechten: RestDocumentRechten? = null,
    var indicatieGebruiksrecht: Boolean = false
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
    creatiedatum = convertToLocalDate(this@toRestDocumentZoekObject.creatiedatum),
    registratiedatum = convertToLocalDate(this@toRestDocumentZoekObject.registratiedatum),
    ontvangstdatum = convertToLocalDate(this@toRestDocumentZoekObject.ontvangstdatum),
    verzenddatum = convertToLocalDate(this@toRestDocumentZoekObject.verzenddatum),
    ondertekeningDatum = convertToLocalDate(this@toRestDocumentZoekObject.ondertekeningDatum),
    vertrouwelijkheidaanduiding = this@toRestDocumentZoekObject.vertrouwelijkheidaanduiding,
    auteur = this@toRestDocumentZoekObject.auteur,
    status = this@toRestDocumentZoekObject.getStatus()?.toString(),
    formaat = this@toRestDocumentZoekObject.formaat,
    versie = this@toRestDocumentZoekObject.versie,
    bestandsnaam = this@toRestDocumentZoekObject.bestandsnaam,
    bestandsomvang = this@toRestDocumentZoekObject.bestandsomvang,
    documentType = this@toRestDocumentZoekObject.documentType,
    ondertekeningSoort = this@toRestDocumentZoekObject.ondertekeningSoort,
    indicatieOndertekend = this@toRestDocumentZoekObject.isIndicatie(DocumentIndicatie.ONDERTEKEND),
    inhoudUrl = this@toRestDocumentZoekObject.inhoudUrl,
    indicatieVergrendeld = this@toRestDocumentZoekObject.isIndicatie(DocumentIndicatie.VERGRENDELD),
    vergrendeldDoor = this@toRestDocumentZoekObject.vergrendeldDoorNaam,
    indicaties = this@toRestDocumentZoekObject.getDocumentIndicaties()
        .filter { it != DocumentIndicatie.GEBRUIKSRECHT }
        .toCollection(EnumSet.noneOf(DocumentIndicatie::class.java)),
    rechten = RestRechtenConverter.convert(documentRechten)
)
