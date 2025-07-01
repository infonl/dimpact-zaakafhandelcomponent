/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.ztc.model

import nl.info.client.zgw.ztc.model.generated.AfleidingswijzeEnum
import nl.info.client.zgw.ztc.model.generated.ArchiefnominatieEnum
import nl.info.client.zgw.ztc.model.generated.BesluitType
import nl.info.client.zgw.ztc.model.generated.BrondatumArchiefprocedure
import nl.info.client.zgw.ztc.model.generated.Catalogus
import nl.info.client.zgw.ztc.model.generated.InformatieObjectType
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import nl.info.client.zgw.ztc.model.generated.ReferentieProces
import nl.info.client.zgw.ztc.model.generated.ResultaatType
import nl.info.client.zgw.ztc.model.generated.RolType
import nl.info.client.zgw.ztc.model.generated.StatusType
import nl.info.client.zgw.ztc.model.generated.VertrouwelijkheidaanduidingEnum
import nl.info.client.zgw.ztc.model.generated.ZaakType
import java.net.URI
import java.time.LocalDate
import java.util.UUID

@Suppress("LongParameterList")
fun createBesluitType(
    url: URI = URI("http://example.com/zaaktype/${UUID.randomUUID()}"),
    zaaktypen: List<URI> = listOf(URI("fakeZaaktype1"), URI("fakeZaaktype2")),
    isConcept: Boolean = false,
    resultaattypen: List<URI> = listOf(URI("fakeResultaatType1"), URI("fakeResultaatType2")),
    resultaattypenOmschrijving: List<String> =
        listOf("fakeResultaatTypeOmschrijving1", "fakeResultaatTypeOmschrijving2"),
    vastgelegdIn: List<String> = listOf("fakeVastgelegdIn1", "fakeVastgelegdIn2"),
    beginObject: LocalDate = LocalDate.now(),
    eindeObject: LocalDate = LocalDate.now().plusDays(1),
    informatieobjecttypen: List<URI>? = listOf(URI("fakeInformatieObjectType1"), URI("fakeInformatieObjectType2")),
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

fun createBrondatumArchiefprocedure(
    afleidingswijze: AfleidingswijzeEnum? = AfleidingswijzeEnum.VERVALDATUM_BESLUIT
) = BrondatumArchiefprocedure().apply {
    this.afleidingswijze = afleidingswijze
}

fun createCatalogus(
    url: URI = URI("http://example.com/catalogus/${UUID.randomUUID()}"),
    zaaktypen: List<URI> = emptyList(),
    besluittypen: List<URI> = emptyList(),
    informatieobjecttypen: List<URI> = emptyList()
) = Catalogus(
    url,
    zaaktypen,
    besluittypen,
    informatieobjecttypen
)

fun createCatalogusListParameters(
    domein: String? = null,
    domeinIn: String? = null,
    rsin: String? = null,
    rsinIn: String? = null,
    page: Int? = null
) = CatalogusListParameters().apply {
    this.domein = domein
    this.domeinIn = domeinIn
    this.rsin = rsin
    this.rsinIn = rsinIn
    this.page = page
}

@Suppress("LongParameterList")
fun createRolType(
    beginObject: LocalDate = LocalDate.now(),
    catalogusUri: URI = URI("https://example.com/catalogus/${UUID.randomUUID()}"),
    eindeObject: LocalDate = LocalDate.now().plusDays(1),
    omschrijving: String = "fakeOmschrijving",
    omschrijvingGeneriek: OmschrijvingGeneriekEnum = OmschrijvingGeneriekEnum.INITIATOR,
    uri: URI = URI("https://example.com/roltype/${UUID.randomUUID()}"),
    zaaktypeIdentificatie: String = "fakeZaaktypeIdentificatie",
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
    omschrijving: String = "fakeZaakTypeOmschrijving",
    informatieObjectTypen: List<URI>? = listOf(URI("fakeInformatieObjectType1"), URI("fakeInformatieObjectType2")),
    identification: String = "fakeIdentificatie",
    besluittypen: List<URI>? = null,
    resultTypes: List<URI>? = listOf(URI("fakeResultaatType1"), URI("fakeResultaatType2")),
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
    listOf(URI("fakeStatusType1"), URI("fakeStatusType2")),
    resultTypes,
    listOf(URI("fakeEigenschap1"), URI("fakeEigenschap2")), informatieObjectTypen,
    listOf(URI("fakeRolType1"), URI("fakeRolType2")),
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
    omschrijving: String = "fakeOmschrijving",
    vertrouwelijkheidaanduiding: VertrouwelijkheidaanduidingEnum = VertrouwelijkheidaanduidingEnum.OPENBAAR,
    beginGeldigheid: LocalDate = LocalDate.now(),
    concept: Boolean = false,
) = InformatieObjectType(
    uri,
    concept,
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
    name: String = "fakeNaam",
    uri: URI = URI("http://example.com/referentieproces/${UUID.randomUUID()}")
) = ReferentieProces().apply {
    this.naam = name
    this.link = uri
}

@Suppress("LongParameterList")
fun createResultaatType(
    url: URI = URI("http://example.com/zaaktype/${UUID.randomUUID()}"),
    zaaktypeIdentificatie: String = "fakeZaaktypeIdentificatie",
    omschrijvingGeneriek: String = "fakeOmschrijvingGeneriek",
    catalogus: URI = URI("http://example.com/catalogus${UUID.randomUUID()}"),
    besluittypeOmschrijving: MutableList<String> = mutableListOf("fakeBesluittypeOmschrijving"),
    informatieobjecttypeOmschrijving: MutableList<String> = mutableListOf("fakeInformatieobjecttypeOmschrijving"),
    beginObject: LocalDate = LocalDate.now(),
    eindeObject: LocalDate = LocalDate.now().plusDays(1),
    archiefnominatie: ArchiefnominatieEnum = ArchiefnominatieEnum.BLIJVEND_BEWAREN,
    archiefactietermijn: String? = null,
    brondatumArchiefprocedure: BrondatumArchiefprocedure? = null,
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
    this.brondatumArchiefprocedure = brondatumArchiefprocedure
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
    omschrijving: String? = null,
    volgnummer: Int? = 1
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
    this.volgnummer = volgnummer
}
