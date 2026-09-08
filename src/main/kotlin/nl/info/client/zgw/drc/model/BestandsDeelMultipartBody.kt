/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.drc.model

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.SequenceInputStream
import java.util.Collections
import java.util.UUID

/**
 * The body of a `PUT /bestandsdelen/{uuid}` request, assembled by hand.
 *
 * The obvious alternative, handing RESTEasy a `MultipartFormDataOutput`, holds the part it writes in
 * memory. A documents registry is free to announce a single bestandsdeel covering the whole
 * document, and Open Zaak does exactly that unless `DOCUMENTEN_UPLOAD_CHUNK_SIZE` is lowered, so the
 * size of a part is the size of the document. Assembling the body here means it can be handed over
 * as a stream, and the part travels from the content stream to the request without ever being held
 * on the heap.
 */
class BestandsDeelMultipartBody(private val partContent: InputStream, lock: String) {
    companion object {
        private const val CRLF = "\r\n"
    }

    private val boundary = "zac-bestandsdeel-${UUID.randomUUID()}"

    val contentType = "multipart/form-data; boundary=$boundary"

    private val beforePartContent = (
        "--$boundary$CRLF" +
            """Content-Disposition: form-data; name="inhoud"; filename="bestandsdeel"$CRLF""" +
            "Content-Type: application/octet-stream$CRLF$CRLF"
        ).toAsciiByteArray()

    private val afterPartContent = (
        CRLF +
            "--$boundary$CRLF" +
            """Content-Disposition: form-data; name="lock"$CRLF$CRLF""" +
            "$lock$CRLF" +
            "--$boundary--$CRLF"
        ).toAsciiByteArray()

    fun inputStream(): InputStream = SequenceInputStream(
        Collections.enumeration(
            listOf(
                ByteArrayInputStream(beforePartContent),
                partContent,
                ByteArrayInputStream(afterPartContent)
            )
        )
    )
}

private fun String.toAsciiByteArray() = toByteArray(Charsets.US_ASCII)
