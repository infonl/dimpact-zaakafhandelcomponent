/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.configuration

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import nl.info.zac.configuration.FileSizeConfiguration.Companion.BYTES_PER_MB
import nl.info.zac.configuration.FileSizeConfiguration.Companion.IN_MEMORY_OPERATION_HEAP_BUDGET_FRACTION
import nl.info.zac.configuration.FileSizeConfiguration.Companion.IN_MEMORY_OPERATION_HEAP_FACTOR
import nl.info.zac.configuration.exception.FileSizeExceededException
import nl.info.zac.configuration.exception.FileTooLargeToOpenException
import nl.info.zac.configuration.exception.InvalidFileSizeConfigurationException
import java.io.ByteArrayInputStream

class FileSizeConfigurationTest : BehaviorSpec({
    val maxHeapMB = Runtime.getRuntime().maxMemory() / BYTES_PER_MB
    val largestSupportedInMemoryFileSizeMB =
        (maxHeapMB * IN_MEMORY_OPERATION_HEAP_BUDGET_FRACTION).toLong() / IN_MEMORY_OPERATION_HEAP_FACTOR

    given("a maximum file size of 500 MB and an in-memory maximum that fits in the heap") {
        val fileSizeConfiguration = FileSizeConfiguration(
            maxFileSizeMB = 500L,
            maxInMemoryFileSizeMB = largestSupportedInMemoryFileSizeMB
        )

        `when`("the configuration is validated on startup") {
            then("it is accepted") {
                shouldNotThrowAny { fileSizeConfiguration.onStartup(Any()) }
            }
        }

        `when`("a document of exactly the maximum file size is offered") {
            then("it is allowed") {
                shouldNotThrowAny { fileSizeConfiguration.assertFileSizeAllowed(500L * BYTES_PER_MB) }
            }
        }

        `when`("a document larger than the maximum file size is offered") {
            val fileSizeExceededException = shouldThrow<FileSizeExceededException> {
                fileSizeConfiguration.assertFileSizeAllowed(500L * BYTES_PER_MB + 1)
            }

            then("it is refused") {
                fileSizeExceededException.message shouldContain "MAX_FILE_SIZE_MB=500"
            }
        }

        `when`("a document larger than the in-memory maximum has to be held in memory") {
            val fileTooLargeToOpenException = shouldThrow<FileTooLargeToOpenException> {
                fileSizeConfiguration.assertFileCanBeHeldInMemory(
                    largestSupportedInMemoryFileSizeMB * BYTES_PER_MB + 1
                )
            }

            then("it is refused so that the operation cannot run the heap out of memory") {
                fileTooLargeToOpenException.message shouldContain "MAX_IN_MEMORY_FILE_SIZE_MB"
            }
        }

        `when`("it is decided how a document is uploaded") {
            then("only a document beyond the in-memory maximum is uploaded in parts") {
                fileSizeConfiguration.isUploadedInParts(
                    largestSupportedInMemoryFileSizeMB * BYTES_PER_MB
                ) shouldBe false
                fileSizeConfiguration.isUploadedInParts(
                    largestSupportedInMemoryFileSizeMB * BYTES_PER_MB + 1
                ) shouldBe true
            }
        }
    }

    given("a maximum in-memory file size of 1 MB") {
        val fileSizeConfiguration = FileSizeConfiguration(maxFileSizeMB = 500L, maxInMemoryFileSizeMB = 1L)

        `when`("content of exactly that size is read into memory") {
            val bytes = ByteArray(BYTES_PER_MB.toInt()) { it.toByte() }

            then("it is returned in full") {
                fileSizeConfiguration.readWithinInMemoryLimit(ByteArrayInputStream(bytes)) shouldBe bytes
            }
        }

        `when`("content beyond that size is read into memory") {
            val fileTooLargeToOpenException = shouldThrow<FileTooLargeToOpenException> {
                fileSizeConfiguration.readWithinInMemoryLimit(
                    ByteArrayInputStream(ByteArray(BYTES_PER_MB.toInt() + 1))
                )
            }

            then("it is refused on the bytes that arrive, so that content of unreported size cannot exhaust the heap") {
                fileTooLargeToOpenException.message shouldContain "MAX_IN_MEMORY_FILE_SIZE_MB=1"
            }
        }
    }

    given("an in-memory maximum that does not fit in the available heap") {
        val fileSizeConfiguration = FileSizeConfiguration(
            maxFileSizeMB = maxHeapMB * IN_MEMORY_OPERATION_HEAP_FACTOR,
            maxInMemoryFileSizeMB = maxHeapMB * IN_MEMORY_OPERATION_HEAP_FACTOR
        )

        `when`("the configuration is validated on startup") {
            val invalidFileSizeConfigurationException = shouldThrow<InvalidFileSizeConfigurationException> {
                fileSizeConfiguration.onStartup(Any())
            }

            then("ZAC refuses to start and names the value it can support") {
                invalidFileSizeConfigurationException.message shouldContain "MAX_IN_MEMORY_FILE_SIZE_MB"
                invalidFileSizeConfigurationException.message shouldContain
                    "Either lower MAX_IN_MEMORY_FILE_SIZE_MB to at most $largestSupportedInMemoryFileSizeMB MB"
            }
        }
    }

    given("an in-memory maximum larger than the maximum file size") {
        val fileSizeConfiguration = FileSizeConfiguration(maxFileSizeMB = 80L, maxInMemoryFileSizeMB = 100L)

        `when`("the configuration is validated on startup") {
            val invalidFileSizeConfigurationException = shouldThrow<InvalidFileSizeConfigurationException> {
                fileSizeConfiguration.onStartup(Any())
            }

            then("ZAC refuses to start because a document that cannot be stored cannot be opened either") {
                invalidFileSizeConfigurationException.message shouldBe
                    "MAX_IN_MEMORY_FILE_SIZE_MB (100) cannot be larger than MAX_FILE_SIZE_MB (80)"
            }
        }
    }

    given("a maximum file size of zero") {
        val fileSizeConfiguration = FileSizeConfiguration(maxFileSizeMB = 0L, maxInMemoryFileSizeMB = 0L)

        `when`("the configuration is validated on startup") {
            val invalidFileSizeConfigurationException = shouldThrow<InvalidFileSizeConfigurationException> {
                fileSizeConfiguration.onStartup(Any())
            }

            then("ZAC refuses to start") {
                invalidFileSizeConfigurationException.message shouldContain "must both be greater than zero"
            }
        }
    }
})
