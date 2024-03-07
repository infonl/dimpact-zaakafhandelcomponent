/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.aanvraag

import net.atos.zac.aanvraag.model.generated.Data
import net.atos.zac.aanvraag.model.generated.ProductaanvraagDenhaag
import java.net.URI

@Suppress("LongParameterList")
fun createProductaanvraagDenhaag(
    data: Data = Data(),
    type: String = "dummyType",
    bsn: String = "dummyBsn",
    pdfUrl: URI = URI("http://example.com/dummyPdf"),
    csvUrl: URI = URI("http://example.com/dummyCsv"),
    attachments: List<URI> = listOf(
        URI("http://example.com/dummyAttachment1"),
        URI("http://example.com/dummyAttachment2")
    )
) =
    ProductaanvraagDenhaag().apply {
        this.data = data
        this.type = type
        this.bsn = bsn
        this.pdfUrl = pdfUrl
        this.csvUrl = csvUrl
        this.attachments = attachments
    }
