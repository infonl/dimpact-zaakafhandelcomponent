/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.drc.model

import java.io.InputStream

class BestandsDeelInputStream(
    private val content: InputStream,
    private val sizeInBytes: Int
) : InputStream() {
    var bytesRead = 0
        private set

    override fun read(): Int =
        if (bytesRead == sizeInBytes) {
            -1
        } else {
            content.read().also { if (it != -1) bytesRead++ }
        }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        if (bytesRead == sizeInBytes) {
            -1
        } else {
            content.read(buffer, offset, minOf(length, sizeInBytes - bytesRead))
                .also { if (it != -1) bytesRead += it }
        }

    override fun available() = minOf(content.available(), sizeInBytes - bytesRead)

    override fun close() = Unit
}
