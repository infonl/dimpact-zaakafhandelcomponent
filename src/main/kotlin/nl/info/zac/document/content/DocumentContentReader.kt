/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.document.content

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import nl.info.zac.configuration.FileSizeConfiguration
import nl.info.zac.configuration.exception.FileSizeExceededException
import nl.info.zac.exception.ErrorCode.ERROR_CODE_DOCUMENT_UPLOAD_INVALID
import nl.info.zac.exception.InputValidationFailedException
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.io.InputStream
import java.nio.file.Files

/**
 * Reads an uploaded document into a [DocumentContent], enforcing the configured maximum file size
 * while doing so.
 *
 * This is where the server-side size limit is actually applied. The frontend refuses oversized
 * documents too, but that check is advisory: it is the byte count observed here that decides,
 * because a client can send anything.
 */
@ApplicationScoped
@NoArgConstructor
@AllOpen
class DocumentContentReader @Inject constructor(
    private val fileSizeConfiguration: FileSizeConfiguration
) {
    companion object {
        private const val TEMPORARY_FILE_PREFIX = "zac-document-upload-"
        private const val COPY_BUFFER_SIZE = 64 * 1024
    }

    /**
     * Reads [inputStream] completely and returns its content, backed by memory when it fits in the
     * configured in-memory budget and by a temporary file when it does not.
     *
     * @throws FileSizeExceededException when the document is larger than the configured maximum.
     * @throws InputValidationFailedException when the document is empty.
     */
    fun read(inputStream: InputStream): DocumentContent {
        val inMemoryLimit = fileSizeConfiguration.maxInMemoryFileSizeBytes
            .coerceAtMost(Int.MAX_VALUE.toLong() - Byte.MAX_VALUE)
            .toInt()
        // read one byte beyond the limit so that a document of exactly the limit still stays in memory
        val head = inputStream.readNBytes(inMemoryLimit + 1)
        return if (head.size <= inMemoryLimit) {
            if (head.isEmpty()) {
                throw InputValidationFailedException(
                    errorCode = ERROR_CODE_DOCUMENT_UPLOAD_INVALID,
                    message = "An empty document cannot be uploaded"
                )
            }
            fileSizeConfiguration.assertFileSizeAllowed(head.size.toLong())
            InMemoryDocumentContent(head)
        } else {
            spillToTemporaryFile(head = head, remainder = inputStream)
        }
    }

    private fun spillToTemporaryFile(head: ByteArray, remainder: InputStream): DocumentContent {
        val path = Files.createTempFile(TEMPORARY_FILE_PREFIX, null)
        var isSpilled = false
        try {
            var bytesWritten = head.size.toLong()
            fileSizeConfiguration.assertFileSizeAllowed(bytesWritten)
            Files.newOutputStream(path).use { outputStream ->
                outputStream.write(head)
                val buffer = ByteArray(COPY_BUFFER_SIZE)
                while (true) {
                    val bytesRead = remainder.read(buffer)
                    if (bytesRead == -1) break
                    bytesWritten += bytesRead
                    fileSizeConfiguration.assertFileSizeAllowed(bytesWritten)
                    outputStream.write(buffer, 0, bytesRead)
                }
            }
            isSpilled = true
            return TemporaryFileDocumentContent(path)
        } finally {
            if (!isSpilled) {
                Files.deleteIfExists(path)
            }
        }
    }
}
