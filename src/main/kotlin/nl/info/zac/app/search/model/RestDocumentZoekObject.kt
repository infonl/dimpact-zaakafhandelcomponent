/*
 *  SPDX-FileCopyrightText: 2025 Lifely
 *  SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.search.model

import net.atos.zac.app.policy.converter.RestRechtenConverter
import net.atos.zac.app.policy.model.RestDocumentRechten
import net.atos.zac.policy.output.DocumentRechten
import net.atos.zac.search.model.DocumentIndicatie
import net.atos.zac.search.model.zoekobject.DocumentZoekObject
import net.atos.zac.search.model.zoekobject.ZoekObjectType
import net.atos.zac.util.time.DateTimeConverterUtil.convertToLocalDate
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
    val indicatieOndertekend: Boolean = false,
    val inhoudUrl: String? = null,
    val indicatieVergrendeld: Boolean = false,
    val vergrendeldDoor: String? = null,
    val indicaties: EnumSet<DocumentIndicatie>? = null,
    val rechten: RestDocumentRechten? = null,
    val indicatieGebruiksrecht: Boolean = false
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
