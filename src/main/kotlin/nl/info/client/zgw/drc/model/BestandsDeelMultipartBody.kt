/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.drc.model

import java.io.ByteArrayOutputStream
import java.util.UUID

/**
 * The body of a `PUT /bestandsdelen/{uuid}` request, assembled by hand.
 *
 * The obvious alternative, handing RESTEasy a `MultipartFormDataOutput`, produces a request body of
 * unknown length, which the client then sends with `Transfer-Encoding: chunked`. The documents
 * registry runs behind uwsgi, which does not buffer a chunked body before passing it to Django, so
 * it arrives with no fields at all and is rejected. Assembling the body here means it goes out as a
 * byte array of known length, with a `Content-Length` and no chunking.
 */
class BestandsDeelMultipartBody(private val partContent: ByteArray, private val lock: String) {
    companion object {
        private const val CRLF = "\r\n"
    }

    private val boundary = "zac-bestandsdeel-${UUID.randomUUID()}"

    val contentType = "multipart/form-data; boundary=$boundary"

    fun toByteArray(): ByteArray = ByteArrayOutputStream().apply {
        writeAscii("--$boundary$CRLF")
        writeAscii("""Content-Disposition: form-data; name="inhoud"; filename="bestandsdeel"$CRLF""")
        writeAscii("Content-Type: application/octet-stream$CRLF$CRLF")
        write(partContent)
        writeAscii(CRLF)
        writeAscii("--$boundary$CRLF")
        writeAscii("""Content-Disposition: form-data; name="lock"$CRLF$CRLF""")
        writeAscii(lock)
        writeAscii(CRLF)
        writeAscii("--$boundary--$CRLF")
    }.toByteArray()

    private fun ByteArrayOutputStream.writeAscii(text: String) = write(text.toByteArray(Charsets.US_ASCII))
}
