/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.document.content

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import nl.info.zac.configuration.FileSizeConfiguration
import nl.info.zac.configuration.FileSizeConfiguration.Companion.BYTES_PER_MB
import nl.info.zac.configuration.exception.FileSizeExceededException
import nl.info.zac.exception.InputValidationFailedException
import java.io.ByteArrayInputStream

class DocumentContentReaderTest : BehaviorSpec({
    val documentContentReader = DocumentContentReader(
        FileSizeConfiguration(maxFileSizeMB = 4L, maxInMemoryFileSizeMB = 1L)
    )

    given("a document that fits in the in-memory maximum") {
        val bytes = ByteArray(BYTES_PER_MB.toInt()) { it.toByte() }

        `when`("it is read") {
            val documentContent = documentContentReader.read(ByteArrayInputStream(bytes))

            then("it is kept in memory") {
                documentContent.shouldBeInstanceOf<InMemoryDocumentContent>()
                documentContent.sizeInBytes shouldBe BYTES_PER_MB
                documentContent.inputStream().readBytes() shouldBe bytes
            }
        }
    }

    given("a document larger than the in-memory maximum but within the maximum file size") {
        val bytes = ByteArray(BYTES_PER_MB.toInt() * 3) { it.toByte() }

        `when`("it is read") {
            val documentContent = documentContentReader.read(ByteArrayInputStream(bytes))

            then("it is spilled to a temporary file that can be streamed more than once") {
                documentContent.shouldBeInstanceOf<TemporaryFileDocumentContent>()
                documentContent.sizeInBytes shouldBe bytes.size.toLong()
                documentContent.inputStream().readBytes() shouldBe bytes
                documentContent.inputStream().readBytes() shouldBe bytes
            }

            and("closing it removes the temporary file") {
                documentContent.close()

                shouldThrow<Exception> { documentContent.inputStream() }
            }
        }
    }

    given("a document larger than the maximum file size") {
        val bytes = ByteArray(BYTES_PER_MB.toInt() * 5)

        `when`("it is read") {
            val fileSizeExceededException = shouldThrow<FileSizeExceededException> {
                documentContentReader.read(ByteArrayInputStream(bytes))
            }

            then("it is refused regardless of what the client claimed the size was") {
                fileSizeExceededException.message shouldContain "MAX_FILE_SIZE_MB=4"
            }
        }
    }

    given("an empty document") {
        `when`("it is read") {
            val inputValidationFailedException = shouldThrow<InputValidationFailedException> {
                documentContentReader.read(ByteArrayInputStream(ByteArray(0)))
            }

            then("it is refused") {
                inputValidationFailedException.message shouldBe "An empty document cannot be uploaded"
            }
        }
    }
})
