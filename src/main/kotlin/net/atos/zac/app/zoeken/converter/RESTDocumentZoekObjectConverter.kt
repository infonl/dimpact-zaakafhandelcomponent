/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zoeken.converter

import net.atos.zac.app.policy.converter.RestRechtenConverter
import net.atos.zac.app.zoeken.model.RESTDocumentZoekObject
import net.atos.zac.policy.output.DocumentRechten
import net.atos.zac.util.time.DateTimeConverterUtil
import net.atos.zac.zoeken.model.DocumentIndicatie
import net.atos.zac.zoeken.model.zoekobject.DocumentZoekObject
import java.util.EnumSet
import java.util.function.Supplier
import java.util.stream.Collectors

fun convertDocumentZoekObject(documentZoekObject: DocumentZoekObject, documentRechten: DocumentRechten): RESTDocumentZoekObject {
    val restDocumentZoekObject = RESTDocumentZoekObject()
    restDocumentZoekObject.id = documentZoekObject.getObjectId()
    restDocumentZoekObject.type = documentZoekObject.getType()
    restDocumentZoekObject.titel = documentZoekObject.titel
    restDocumentZoekObject.beschrijving = documentZoekObject.beschrijving
    restDocumentZoekObject.zaaktypeUuid = documentZoekObject.zaaktypeUuid
    restDocumentZoekObject.zaaktypeIdentificatie = documentZoekObject.zaaktypeIdentificatie
    restDocumentZoekObject.zaaktypeOmschrijving = documentZoekObject.zaaktypeOmschrijving
    restDocumentZoekObject.zaakIdentificatie = documentZoekObject.zaakIdentificatie
    restDocumentZoekObject.zaakUuid = documentZoekObject.zaakUuid
    restDocumentZoekObject.zaakRelatie = documentZoekObject.zaakRelatie
    restDocumentZoekObject.creatiedatum = DateTimeConverterUtil.convertToLocalDate(documentZoekObject.creatiedatum)
    restDocumentZoekObject.registratiedatum =
        DateTimeConverterUtil.convertToLocalDate(documentZoekObject.registratiedatum)
    restDocumentZoekObject.ontvangstdatum =
        DateTimeConverterUtil.convertToLocalDate(documentZoekObject.ontvangstdatum)
    restDocumentZoekObject.verzenddatum = DateTimeConverterUtil.convertToLocalDate(documentZoekObject.verzenddatum)
    restDocumentZoekObject.ondertekeningDatum =
        DateTimeConverterUtil.convertToLocalDate(documentZoekObject.ondertekeningDatum)
    restDocumentZoekObject.vertrouwelijkheidaanduiding = documentZoekObject.vertrouwelijkheidaanduiding
    restDocumentZoekObject.auteur = documentZoekObject.auteur
    if (documentZoekObject.getStatus() != null) {
        restDocumentZoekObject.status = documentZoekObject.getStatus().toString()
    }
    restDocumentZoekObject.formaat = documentZoekObject.formaat
    restDocumentZoekObject.versie = documentZoekObject.versie
    restDocumentZoekObject.bestandsnaam = documentZoekObject.bestandsnaam
    restDocumentZoekObject.bestandsomvang = documentZoekObject.bestandsomvang
    restDocumentZoekObject.documentType = documentZoekObject.documentType
    restDocumentZoekObject.ondertekeningSoort = documentZoekObject.ondertekeningSoort
    restDocumentZoekObject.indicatieOndertekend = documentZoekObject.isIndicatie(DocumentIndicatie.ONDERTEKEND)
    restDocumentZoekObject.inhoudUrl = documentZoekObject.inhoudUrl
    restDocumentZoekObject.indicatieVergrendeld = documentZoekObject.isIndicatie(DocumentIndicatie.VERGRENDELD)
    restDocumentZoekObject.vergrendeldDoor = documentZoekObject.vergrendeldDoorNaam
    restDocumentZoekObject.indicaties = documentZoekObject.getDocumentIndicaties().stream()
        .filter { indicatie: DocumentIndicatie? -> indicatie != DocumentIndicatie.GEBRUIKSRECHT }
        .collect(Collectors.toCollection(Supplier { EnumSet.noneOf<DocumentIndicatie?>(DocumentIndicatie::class.java) }))
    restDocumentZoekObject.rechten = RestRechtenConverter.convert(documentRechten)
    return restDocumentZoekObject
}
