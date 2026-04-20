/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag.model

import nl.info.zac.productaanvraag.model.generated.Betrokkene
import nl.info.zac.productaanvraag.model.generated.Bron
import nl.info.zac.productaanvraag.model.generated.ProductaanvraagDimpact
import nl.info.zac.search.model.DatumRange
import java.net.URI
import java.time.LocalDate
import java.util.UUID

fun createBetrokkene(
    inBsn: String? = "fakeBsn",
    vestigingsNummer: String? = "fakeVestigingsNummer",
    roltypeOmschrijving: String = "fakeRoltypeOmschrijving",
    kvkNummer: String? = "fakeKvkNummer"
) =
    Betrokkene().apply {
        this.inpBsn = inBsn
        this.vestigingsNummer = vestigingsNummer
        this.roltypeOmschrijving = roltypeOmschrijving
        this.kvkNummer = kvkNummer
    }

fun createBron(
    naam: String = "fakeNaam",
    kenmerk: String = "fakeKenmerk"
) = Bron().apply {
    this.naam = naam
    this.kenmerk = kenmerk
}

@Suppress("LongParameterList")
fun createInboxProductaanvraag(
    id: Long = 1L,
    productaanvraagObjectUUID: UUID = UUID.randomUUID(),
    aanvraagdocumentUUID: UUID? = null,
    ontvangstdatum: LocalDate = LocalDate.now(),
    type: String = "fakeType",
    initiatorID: String? = null,
    aantalBijlagen: Int = 0
) = InboxProductaanvraag().apply {
    this.id = id
    this.productaanvraagObjectUUID = productaanvraagObjectUUID
    this.aanvraagdocumentUUID = aanvraagdocumentUUID
    this.ontvangstdatum = ontvangstdatum
    this.type = type
    this.initiatorID = initiatorID
    this.aantalBijlagen = aantalBijlagen
}

fun createInboxProductaanvraagListParameters(
    initiatorID: String? = null,
    type: String? = null,
    ontvangstdatumRange: DatumRange? = null
) = InboxProductaanvraagListParameters().apply {
    this.initiatorID = initiatorID
    this.type = type
    this.ontvangstdatum = ontvangstdatumRange
}

@Suppress("LongParameterList")
fun createProductaanvraagDimpact(
    type: String = "fakeType",
    betrokkenen: List<Betrokkene> = listOf(createBetrokkene()),
    pdfUrl: URI = URI("http://example.com/fakePdf"),
    csvUrl: URI = URI("http://example.com/fakeCsv"),
    attachments: List<URI> = listOf(
        URI("http://example.com/fakeAttachment1"),
        URI("http://example.com/fakeAttachment2")
    )
) =
    ProductaanvraagDimpact().apply {
        this.type = type
        this.betrokkenen = betrokkenen
        this.pdf = pdfUrl
        this.csv = csvUrl
        this.bijlagen = attachments
    }
