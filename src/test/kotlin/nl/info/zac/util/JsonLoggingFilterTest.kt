/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.ws.rs.client.ClientRequestContext
import jakarta.ws.rs.client.ClientResponseContext
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.MultivaluedHashMap
import jakarta.ws.rs.core.Response
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

class JsonLoggingFilterTest : BehaviorSpec({
    val jsonLoggingFilter = JsonLoggingFilter()

    afterEach {
        checkUnnecessaryStub()
    }

    context("Filtering a REST request") {
        given("the log level of the filter is higher than FINE") {
            val clientRequestContext = mockk<ClientRequestContext>()

            `when`("the request is filtered") {
                val logRecords = captureLogRecords(Level.INFO) { jsonLoggingFilter.filter(clientRequestContext) }

                then("nothing is logged and the request is not read") {
                    logRecords.shouldBeEmpty()
                    verify(exactly = 0) { clientRequestContext.uri }
                }
            }
        }

        given("the log level of the filter is FINE and the request has a JSON entity") {
            val clientRequestContext = mockk<ClientRequestContext>()
            every { clientRequestContext.uri } returns URI("https://example.com/fakePath")
            every { clientRequestContext.method } returns "POST"
            every { clientRequestContext.mediaType } returns MediaType.APPLICATION_JSON_TYPE
            every { clientRequestContext.headers } returns MultivaluedHashMap<String, Any>().apply {
                add("fakeHeader", "fakeHeaderValue")
            }
            every { clientRequestContext.hasEntity() } returns true
            every { clientRequestContext.entity } returns mapOf("fakeKey" to "fakeValue")

            `when`("the request is filtered") {
                val logRecords = captureLogRecords(Level.FINE) { jsonLoggingFilter.filter(clientRequestContext) }

                then("the URL, method, media type, headers and formatted payload are logged") {
                    with(logRecords.single()) {
                        level shouldBe Level.FINE
                        message shouldContain "REST Request"
                        message shouldContain "URL: https://example.com/fakePath"
                        message shouldContain "Method: POST"
                        message shouldContain "Media type: application/json"
                        message shouldContain "fakeHeader : [fakeHeaderValue]"
                        message shouldContain "Payload:"
                        message shouldContain "\"fakeKey\": \"fakeValue\""
                    }
                }
            }
        }

        given("the log level of the filter is FINE and the request has no entity") {
            val clientRequestContext = mockk<ClientRequestContext>()
            every { clientRequestContext.uri } returns URI("https://example.com/fakePath")
            every { clientRequestContext.method } returns "GET"
            every { clientRequestContext.mediaType } returns null
            every { clientRequestContext.headers } returns MultivaluedHashMap()
            every { clientRequestContext.hasEntity() } returns false

            `when`("the request is filtered") {
                val logRecords = captureLogRecords(Level.FINE) { jsonLoggingFilter.filter(clientRequestContext) }

                then("the request is logged without a payload") {
                    with(logRecords.single().message) {
                        this shouldContain "Method: GET"
                        this shouldNotContain "Payload:"
                    }
                }
            }
        }
    }

    context("Filtering a REST response") {
        given("the log level of the filter is higher than FINE") {
            val clientRequestContext = mockk<ClientRequestContext>()
            val clientResponseContext = mockk<ClientResponseContext>()

            `when`("the response is filtered") {
                val logRecords = captureLogRecords(Level.INFO) {
                    jsonLoggingFilter.filter(clientRequestContext, clientResponseContext)
                }

                then("nothing is logged and the response is not read") {
                    logRecords.shouldBeEmpty()
                    verify(exactly = 0) { clientResponseContext.statusInfo }
                }
            }
        }

        given("the log level of the filter is FINE and the response has a JSON entity") {
            val clientRequestContext = mockk<ClientRequestContext>()
            val clientResponseContext = mockk<ClientResponseContext>()
            val payload = "{\"fakeKey\":\"fakeValue\"}"
            val resetEntityStream = slot<InputStream>()
            every { clientResponseContext.statusInfo } returns Response.Status.OK
            every { clientResponseContext.mediaType } returns MediaType.APPLICATION_JSON_TYPE
            every { clientResponseContext.headers } returns MultivaluedHashMap<String, String>().apply {
                add("fakeHeader", "fakeHeaderValue")
            }
            every { clientResponseContext.hasEntity() } returns true
            every { clientResponseContext.entityStream } returns payload.toInputStream()
            every { clientResponseContext.entityStream = capture(resetEntityStream) } returns Unit

            `when`("the response is filtered") {
                val logRecords = captureLogRecords(Level.FINE) {
                    jsonLoggingFilter.filter(clientRequestContext, clientResponseContext)
                }

                then("the status, media type, headers and pretty printed payload are logged") {
                    with(logRecords.single()) {
                        level shouldBe Level.FINE
                        message shouldContain "REST Response"
                        message shouldContain "Status: 200 (OK)"
                        message shouldContain "Media type: application/json"
                        message shouldContain "fakeHeader : [fakeHeaderValue]"
                        message shouldContain "\"fakeKey\": \"fakeValue\""
                    }
                }

                and("the entity stream is replaced, so that the payload can still be read by the client") {
                    String(resetEntityStream.captured.readAllBytes(), StandardCharsets.UTF_8) shouldBe payload
                }
            }
        }

        given("the log level of the filter is FINE and the response has an entity that is not JSON") {
            val clientRequestContext = mockk<ClientRequestContext>()
            val clientResponseContext = mockk<ClientResponseContext>()
            every { clientResponseContext.statusInfo } returns Response.Status.OK
            every { clientResponseContext.mediaType } returns MediaType.TEXT_PLAIN_TYPE
            every { clientResponseContext.headers } returns MultivaluedHashMap()
            every { clientResponseContext.hasEntity() } returns true
            every { clientResponseContext.entityStream } returns "fakePlainTextPayload".toInputStream()
            every { clientResponseContext.entityStream = any() } returns Unit

            `when`("the response is filtered") {
                val logRecords = captureLogRecords(Level.FINE) {
                    jsonLoggingFilter.filter(clientRequestContext, clientResponseContext)
                }

                then("the payload is logged as is") {
                    logRecords.single().message shouldContain "Payload:fakePlainTextPayload"
                }
            }
        }

        given("the log level of the filter is FINE and the response has no entity") {
            val clientRequestContext = mockk<ClientRequestContext>()
            val clientResponseContext = mockk<ClientResponseContext>()
            every { clientResponseContext.statusInfo } returns Response.Status.NO_CONTENT
            every { clientResponseContext.mediaType } returns null
            every { clientResponseContext.headers } returns MultivaluedHashMap()
            every { clientResponseContext.hasEntity() } returns false

            `when`("the response is filtered") {
                val logRecords = captureLogRecords(Level.FINE) {
                    jsonLoggingFilter.filter(clientRequestContext, clientResponseContext)
                }

                then("the response is logged without a payload") {
                    with(logRecords.single().message) {
                        this shouldContain "Status: 204 (No Content)"
                        this shouldNotContain "Payload:"
                    }
                }
            }
        }

        given("the log level of the filter is FINE and reading the response entity fails") {
            val clientRequestContext = mockk<ClientRequestContext>()
            val clientResponseContext = mockk<ClientResponseContext>()
            every { clientResponseContext.statusInfo } returns Response.Status.OK
            every { clientResponseContext.mediaType } returns MediaType.APPLICATION_JSON_TYPE
            every { clientResponseContext.headers } returns MultivaluedHashMap()
            every { clientResponseContext.hasEntity() } returns true
            every { clientResponseContext.entityStream } returns object : InputStream() {
                override fun read(): Int = throw IOException("fakeIOExceptionMessage")
            }

            `when`("the response is filtered") {
                val runtimeException = shouldThrow<RuntimeException> {
                    captureLogRecords(Level.FINE) {
                        jsonLoggingFilter.filter(clientRequestContext, clientResponseContext)
                    }
                }

                then("an exception is thrown that wraps the IO exception") {
                    runtimeException.message shouldBe "Error logging REST response"
                    runtimeException.cause!!.message shouldBe "fakeIOExceptionMessage"
                }
            }
        }
    }
})

private fun String.toInputStream() = ByteArrayInputStream(toByteArray(StandardCharsets.UTF_8))

private fun captureLogRecords(level: Level, block: () -> Unit): List<LogRecord> {
    val logger = Logger.getLogger(JsonLoggingFilter::class.java.name)
    val originalLevel = logger.level
    val records = mutableListOf<LogRecord>()
    val handler = object : Handler() {
        override fun publish(record: LogRecord) {
            records.add(record)
        }
        override fun flush() = Unit
        override fun close() = Unit
    }
    logger.level = level
    logger.addHandler(handler)
    try {
        block()
    } finally {
        logger.removeHandler(handler)
        logger.level = originalLevel
    }
    return records
}
