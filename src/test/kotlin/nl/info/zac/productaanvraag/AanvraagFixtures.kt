/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.productaanvraag

import net.atos.zac.productaanvraag.model.InboxProductaanvraag
import nl.info.zac.productaanvraag.model.generated.Betrokkene
import nl.info.zac.productaanvraag.model.generated.Bron
import nl.info.zac.productaanvraag.model.generated.ProductaanvraagDimpact
import java.net.URI
import java.time.LocalDate
import java.util.UUID

fun createBetrokkene(
    inBsn: String = "dummyBsn",
    roltypeOmschrijving: String = "dummyRoltypeOmschrijving",
) =
    Betrokkene().apply {
        this.inpBsn = inBsn
        this.roltypeOmschrijving = roltypeOmschrijving
    }

fun createBron(
    naam: String = "dummyNaam",
    kenmerk: String = "dummyKenmerk"
) = Bron().apply {
    this.naam = naam
    this.kenmerk = kenmerk
}

@Suppress("LongParameterList")
fun createInboxProductaanvraag(
    id: Long = 1234L,
    productaanvraagObjectUUID: UUID = UUID.randomUUID(),
    aanvraagdocumentUUID: UUID = UUID.randomUUID(),
    ontvangstdatum: LocalDate = LocalDate.now(),
    type: String = "dummyType",
    initiatorID: String = "dummyInitiator",
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

@Suppress("LongParameterList")
fun createProductaanvraagDimpact(
    type: String = "dummyType",
    betrokkenen: List<Betrokkene> = listOf(createBetrokkene()),
    pdfUrl: URI = URI("http://example.com/dummyPdf"),
    csvUrl: URI = URI("http://example.com/dummyCsv"),
    attachments: List<URI> = listOf(
        URI("http://example.com/dummyAttachment1"),
        URI("http://example.com/dummyAttachment2")
    )
) =
    ProductaanvraagDimpact().apply {
        this.type = type
        this.betrokkenen = betrokkenen
        this.pdf = pdfUrl
        this.csv = csvUrl
        this.bijlagen = attachments
    }
