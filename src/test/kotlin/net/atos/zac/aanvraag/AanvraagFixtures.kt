/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.aanvraag

import net.atos.zac.aanvraag.model.generated.Betrokkene
import net.atos.zac.aanvraag.model.generated.Betrokkene.RolOmschrijvingGeneriek
import net.atos.zac.aanvraag.model.generated.ProductaanvraagDimpact
import java.net.URI

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

fun createBetrokkene(
    inBsn: String = "dummyBsn",
    rolOmschrijvingGeneriek: RolOmschrijvingGeneriek = RolOmschrijvingGeneriek.INITIATOR
) =
    Betrokkene().apply {
        this.inpBsn = inBsn
        this.rolOmschrijvingGeneriek = rolOmschrijvingGeneriek
    }
