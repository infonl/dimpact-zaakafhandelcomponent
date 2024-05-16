/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.aanvraag

import net.atos.zac.aanvraag.model.InboxProductaanvraag
import net.atos.zac.aanvraag.model.generated.Betrokkene
import net.atos.zac.aanvraag.model.generated.Betrokkene.RolOmschrijvingGeneriek
import net.atos.zac.aanvraag.model.generated.Bron
import net.atos.zac.aanvraag.model.generated.ProductaanvraagDimpact
import java.net.URI
import java.time.LocalDate
import java.util.*

fun createBetrokkene(
    inBsn: String = "dummyBsn",
    rolOmschrijvingGeneriek: RolOmschrijvingGeneriek = RolOmschrijvingGeneriek.INITIATOR
) =
    Betrokkene().apply {
        this.inpBsn = inBsn
        this.rolOmschrijvingGeneriek = rolOmschrijvingGeneriek
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
    betrokkenen: List<Betrokkene> = listOf(
        createBetrokkene(
            inBsn = "dummyBsn",
            rolOmschrijvingGeneriek = RolOmschrijvingGeneriek.INITIATOR
        )
    ),
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
