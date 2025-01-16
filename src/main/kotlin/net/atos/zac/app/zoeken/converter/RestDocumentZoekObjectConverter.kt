/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zoeken.converter

import net.atos.zac.app.policy.converter.RestRechtenConverter
import net.atos.zac.app.zoeken.model.RestDocumentZoekObject
import net.atos.zac.policy.output.DocumentRechten
import net.atos.zac.util.time.DateTimeConverterUtil.convertToLocalDate
import net.atos.zac.zoeken.model.DocumentIndicatie
import net.atos.zac.zoeken.model.zoekobject.DocumentZoekObject
import java.util.EnumSet

fun DocumentZoekObject.toRestDocumentZoekObject(documentRechten: DocumentRechten) = RestDocumentZoekObject().apply {
    id = this@toRestDocumentZoekObject.getObjectId()
    type = this@toRestDocumentZoekObject.getType()
    titel = this@toRestDocumentZoekObject.titel
    beschrijving = this@toRestDocumentZoekObject.beschrijving
    zaaktypeUuid = this@toRestDocumentZoekObject.zaaktypeUuid
    zaaktypeIdentificatie = this@toRestDocumentZoekObject.zaaktypeIdentificatie
    zaaktypeOmschrijving = this@toRestDocumentZoekObject.zaaktypeOmschrijving
    zaakIdentificatie = this@toRestDocumentZoekObject.zaakIdentificatie
    zaakUuid = this@toRestDocumentZoekObject.zaakUuid
    zaakRelatie = this@toRestDocumentZoekObject.zaakRelatie
    creatiedatum = convertToLocalDate(this@toRestDocumentZoekObject.creatiedatum)
    registratiedatum = convertToLocalDate(this@toRestDocumentZoekObject.registratiedatum)
    ontvangstdatum = convertToLocalDate(this@toRestDocumentZoekObject.ontvangstdatum)
    verzenddatum = convertToLocalDate(this@toRestDocumentZoekObject.verzenddatum)
    ondertekeningDatum = convertToLocalDate(this@toRestDocumentZoekObject.ondertekeningDatum)
    vertrouwelijkheidaanduiding = this@toRestDocumentZoekObject.vertrouwelijkheidaanduiding
    auteur = this@toRestDocumentZoekObject.auteur
    this@toRestDocumentZoekObject.getStatus()?.let { status = it.toString() }
    formaat = this@toRestDocumentZoekObject.formaat
    versie = this@toRestDocumentZoekObject.versie
    bestandsnaam = this@toRestDocumentZoekObject.bestandsnaam
    bestandsomvang = this@toRestDocumentZoekObject.bestandsomvang
    documentType = this@toRestDocumentZoekObject.documentType
    ondertekeningSoort = this@toRestDocumentZoekObject.ondertekeningSoort
    indicatieOndertekend =
        this@toRestDocumentZoekObject.isIndicatie(DocumentIndicatie.ONDERTEKEND)
    inhoudUrl = this@toRestDocumentZoekObject.inhoudUrl
    indicatieVergrendeld =
        this@toRestDocumentZoekObject.isIndicatie(DocumentIndicatie.VERGRENDELD)
    vergrendeldDoor = this@toRestDocumentZoekObject.vergrendeldDoorNaam
    indicaties = this@toRestDocumentZoekObject.getDocumentIndicaties()
        .filter { it != DocumentIndicatie.GEBRUIKSRECHT }
        .toCollection(EnumSet.noneOf(DocumentIndicatie::class.java))
    rechten = RestRechtenConverter.convert(documentRechten)
}
