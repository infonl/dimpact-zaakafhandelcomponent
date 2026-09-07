/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.document.content

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path

/**
 * The content of a document that is on its way to the documents registry.
 *
 * Small documents are kept in memory, which keeps the single request upload as cheap as it has
 * always been. Documents that do not fit in the configured in-memory budget are spilled to a
 * temporary file so that they can be uploaded in parts without ever being held on the heap in full.
 *
 * Always use it in a `use { }` block: closing a [TemporaryFileDocumentContent] deletes its file.
 */
sealed interface DocumentContent : AutoCloseable {
    val sizeInBytes: Long

    fun inputStream(): InputStream

    override fun close() = Unit
}

class InMemoryDocumentContent(private val bytes: ByteArray) : DocumentContent {
    override val sizeInBytes = bytes.size.toLong()

    fun toByteArray() = bytes

    override fun inputStream(): InputStream = ByteArrayInputStream(bytes)
}

class TemporaryFileDocumentContent(private val path: Path) : DocumentContent {
    override val sizeInBytes = Files.size(path)

    override fun inputStream(): InputStream = Files.newInputStream(path)

    override fun close() {
        Files.deleteIfExists(path)
    }
}
