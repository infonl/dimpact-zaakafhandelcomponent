/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.ztc.model

import net.atos.client.zgw.ztc.model.generated.ArchiefnominatieEnum
import net.atos.client.zgw.ztc.model.generated.BesluitType
import net.atos.client.zgw.ztc.model.generated.InformatieObjectType
import net.atos.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import net.atos.client.zgw.ztc.model.generated.ReferentieProces
import net.atos.client.zgw.ztc.model.generated.ResultaatType
import net.atos.client.zgw.ztc.model.generated.RolType
import net.atos.client.zgw.ztc.model.generated.StatusType
import net.atos.client.zgw.ztc.model.generated.VertrouwelijkheidaanduidingEnum
import net.atos.client.zgw.ztc.model.generated.ZaakType
import java.net.URI
import java.time.LocalDate
import java.util.UUID

@Suppress("LongParameterList")
fun createBesluitType(
    url: URI = URI("http://example.com/zaaktype/${UUID.randomUUID()}"),
    zaaktypen: List<URI> = listOf(URI("dummyZaaktype1"), URI("dummyZaaktype2")),
    isConcept: Boolean = false,
    resultaattypen: List<URI> = listOf(URI("dummyResultaatType1"), URI("dummyResultaatType2")),
    resultaattypenOmschrijving: List<String> =
        listOf("dummyResultaatTypeOmschrijving1", "dummyResultaatTypeOmschrijving2"),
    vastgelegdIn: List<String> = listOf("dummyVastgelegdIn1", "dummyVastgelegdIn2"),
    beginObject: LocalDate = LocalDate.now(),
    eindeObject: LocalDate = LocalDate.now().plusDays(1),
    informatieobjecttypen: List<URI>? = listOf(URI("dummyInformatieObjectType1"), URI("dummyInformatieObjectType2")),
    description: String = "description",
    explanation: String = "explanation",
    publicationEnabled: Boolean = false,
    publicationPeriod: String? = null,
    reactionPeriod: String? = null
) =
    BesluitType(
        url,
        zaaktypen,
        isConcept,
        resultaattypen,
        resultaattypenOmschrijving,
        vastgelegdIn,
        beginObject,
        eindeObject
    ).apply {
        setInformatieobjecttypen(informatieobjecttypen)
        omschrijving = description
        toelichting = explanation
        setPublicatieIndicatie(publicationEnabled)
        publicatietermijn = publicationPeriod
        reactietermijn = reactionPeriod
    }

@Suppress("LongParameterList")
fun createRolType(
    beginObject: LocalDate = LocalDate.now(),
    catalogusUri: URI = URI("https://example.com/catalogus/${UUID.randomUUID()}"),
    eindeObject: LocalDate = LocalDate.now().plusDays(1),
    omschrijving: String = "dummyOmschrijving",
    omschrijvingGeneriek: OmschrijvingGeneriekEnum = OmschrijvingGeneriekEnum.INITIATOR,
    uri: URI = URI("https://example.com/roltype/${UUID.randomUUID()}"),
    zaaktypeIdentificatie: String = "dummyZaaktypeIdentificatie",
    zaakTypeUri: URI = URI("https://example.com/${UUID.randomUUID()}"),
) = RolType(
    uri,
    zaaktypeIdentificatie,
    catalogusUri,
    beginObject,
    eindeObject
).apply {
    this.zaaktype = zaakTypeUri
    this.omschrijving = omschrijving
    this.omschrijvingGeneriek = omschrijvingGeneriek
}

@Suppress("LongParameterList")
fun createZaakType(
    uri: URI = URI("https://example.com/zaaktypes/${UUID.randomUUID()}"),
    omschrijving: String = "dummyZaakTypeOmschrijving",
    informatieObjectTypen: List<URI>? = listOf(URI("dummyInformatieObjectType1"), URI("dummyInformatieObjectType2")),
    identification: String = "dummyIdentificatie",
    besluittypen: List<URI>? = null,
    resultTypes: List<URI>? = listOf(URI("dummyResultaatType1"), URI("dummyResultaatType2")),
    concept: Boolean = false,
    doorloopTijd: String = "P10D",
    servicenorm: String? = null,
    beginGeldigheid: LocalDate = LocalDate.now(),
    eindeGeldigheid: LocalDate? = null,
    referentieProces: ReferentieProces? = null
) = ZaakType(
    uri,
    concept,
    null,
    null,
    listOf(URI("dummyStatusType1"), URI("dummyStatusType2")),
    resultTypes,
    listOf(URI("dummyEigenschap1"), URI("dummyEigenschap2")), informatieObjectTypen,
    listOf(URI("dummyRolType1"), URI("dummyRolType2")),
    null
).apply {
    this.omschrijving = omschrijving
    this.besluittypen = besluittypen
    this.identificatie = identification
    this.doorlooptijd = doorloopTijd
    this.servicenorm = servicenorm
    this.beginGeldigheid = beginGeldigheid
    this.eindeGeldigheid = eindeGeldigheid
    this.referentieproces = referentieProces
}

fun createInformatieObjectType(
    uri: URI = URI("http://example.com/catalogus/${UUID.randomUUID()}"),
    omschrijving: String = "dummyOmschrijving",
    vertrouwelijkheidaanduiding: VertrouwelijkheidaanduidingEnum = VertrouwelijkheidaanduidingEnum.OPENBAAR,
    beginGeldigheid: LocalDate = LocalDate.now()
) = InformatieObjectType(
    uri,
    false,
    null,
    null,
    null,
    null
).apply {
    this.omschrijving = omschrijving
    this.vertrouwelijkheidaanduiding = vertrouwelijkheidaanduiding
    this.beginGeldigheid = beginGeldigheid
}

fun createReferentieProcess(
    name: String = "dummyNaam",
    uri: URI = URI("http://example.com/referentieproces/${UUID.randomUUID()}")
) = ReferentieProces().apply {
    this.naam = name
    this.link = uri
}

@Suppress("LongParameterList")
fun createResultaatType(
    url: URI = URI("http://example.com/zaaktype/${UUID.randomUUID()}"),
    zaaktypeIdentificatie: String = "dummyZaaktypeIdentificatie",
    omschrijvingGeneriek: String = "dummyOmschrijvingGeneriek",
    catalogus: URI = URI("http://example.com/catalogus${UUID.randomUUID()}"),
    besluittypeOmschrijving: MutableList<String> = mutableListOf("dummyBesluittypeOmschrijving"),
    informatieobjecttypeOmschrijving: MutableList<String> = mutableListOf("dummyInformatieobjecttypeOmschrijving"),
    beginObject: LocalDate = LocalDate.now(),
    eindeObject: LocalDate = LocalDate.now().plusDays(1),
    archiefnominatie: ArchiefnominatieEnum = ArchiefnominatieEnum.BLIJVEND_BEWAREN,
    archiefactietermijn: String? = null
) = ResultaatType(
    url,
    zaaktypeIdentificatie,
    omschrijvingGeneriek,
    catalogus,
    besluittypeOmschrijving,
    informatieobjecttypeOmschrijving,
    beginObject,
    eindeObject
).apply {
    this.archiefnominatie = archiefnominatie
    this.archiefactietermijn = archiefactietermijn
}

@Suppress("LongParameterList")
fun createStatusType(
    uri: URI = URI("http://example.com/catalogus/${UUID.randomUUID()}"),
    zaaktypeIdentificatie: String? = null,
    isEindstatus: Boolean = false,
    catalogus: URI = URI("http://example.com/catalogus/${UUID.randomUUID()}"),
    eigenschappen: List<URI> = listOf(URI("http://example.com/catalogus/${UUID.randomUUID()}")),
    zaakobjecttypen: List<URI> = listOf(URI("http://example.com/catalogus/${UUID.randomUUID()}")),
    beginObject: LocalDate = LocalDate.now(),
    eindeObject: LocalDate = LocalDate.now(),
    omschrijving: String? = null
) = StatusType(
    uri,
    zaaktypeIdentificatie,
    isEindstatus,
    catalogus,
    eigenschappen,
    zaakobjecttypen,
    beginObject,
    eindeObject
).apply {
    this.omschrijving = omschrijving
}
