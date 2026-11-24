/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.maps.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.TraceFlags
import io.opentelemetry.api.trace.TraceState
import jakarta.ws.rs.client.ClientRequestContext
import jakarta.ws.rs.client.ClientResponseContext
import nl.info.client.bag.api.AdresApi
import nl.info.client.brp.PersonenApi
import nl.info.zac.log.LogCapture
import java.net.URI
import java.util.logging.Level

class JsonLoggingFilterTest : BehaviorSpec({
    val jsonLoggingFilter = JsonLoggingFilter()
    val personenMethod = PersonenApi::class.java.methods.first { it.name == "personen" }
    val bevraagAdressenMetNumIdMethod = AdresApi::class.java.methods.first { it.name == "bevraagAdressenMetNumId" }

    fun createClientRequestContext(
        method: String,
        uri: String,
        invokedMethod: java.lang.reflect.Method?
    ): ClientRequestContext {
        val properties = mutableMapOf<String, Any?>()
        return mockk<ClientRequestContext>().also { clientRequestContext ->
            every { clientRequestContext.method } returns method
            every { clientRequestContext.uri } returns URI(uri)
            every { clientRequestContext.setProperty(any(), any()) } answers { properties[firstArg()] = secondArg() }
            every { clientRequestContext.getProperty(any()) } answers { properties[firstArg()] }
            properties[JsonLoggingFilter.INVOKED_METHOD_PROPERTY] = invokedMethod
        }
    }

    fun createClientResponseContext(status: Int) =
        mockk<ClientResponseContext>().also { every { it.status } returns status }

    fun callFilter(
        clientRequestContext: ClientRequestContext,
        clientResponseContext: ClientResponseContext,
        level: Level = Level.FINE
    ) = LogCapture(JsonLoggingFilter::class, level).use { logCapture ->
        jsonLoggingFilter.filter(clientRequestContext)
        jsonLoggingFilter.filter(clientRequestContext, clientResponseContext)
        logCapture.records
    }

    afterEach {
        checkUnnecessaryStub()
    }

    context("Filtering an outbound REST call") {
        given("FINE is enabled and a BRP person query by BSN that BRP answers with 200") {
            val clientRequestContext = createClientRequestContext(
                method = "POST",
                uri = "https://brp.example.com/haalcentraal/api/brp/personen",
                invokedMethod = personenMethod
            )
            val clientResponseContext = createClientResponseContext(200)

            `when`("the call is filtered") {
                val records = callFilter(clientRequestContext, clientResponseContext)

                then("one record is logged with method, host, operation, status, outcome and duration, and no path") {
                    with(records.single()) {
                        message shouldBe "Outbound REST call"
                        mdc shouldContainAll mapOf(
                            "event" to "OutboundRestCall",
                            "method" to "POST",
                            "host" to "brp.example.com",
                            "operation" to "PersonenApi.personen",
                            "status" to "200",
                            "outcome" to "SUCCESS"
                        )
                        mdc.getValue("durationMs") shouldMatch Regex("[0-9]+")
                        mdc.getValue("correlationId") shouldMatch Regex("[0-9a-f-]{36}")
                        containsAnywhere("/haalcentraal") shouldBe false
                    }
                }

                and("the body and the headers of the call are not read") {
                    verify(exactly = 0) {
                        clientRequestContext.entity
                        clientRequestContext.headers
                        clientResponseContext.entityStream
                        clientResponseContext.headers
                    }
                }
            }
        }

        given("FINE is enabled and a BAG call with an identifier in its path") {
            val clientRequestContext = createClientRequestContext(
                method = "GET",
                uri = "https://bag.example.com/lvbag/individuelebevragingen/v2/adressen/0363200000123456",
                invokedMethod = bevraagAdressenMetNumIdMethod
            )

            `when`("the call is filtered") {
                val records = callFilter(clientRequestContext, createClientResponseContext(200))

                then("the record names the operation and does not contain the identifier") {
                    with(records.single()) {
                        mdc["operation"] shouldBe "AdresApi.bevraagAdressenMetNumId"
                        containsAnywhere("0363200000123456") shouldBe false
                    }
                }
            }
        }

        given("FINE is enabled and a call with a BSN in its query string that the remote system answers with 500") {
            val clientRequestContext = createClientRequestContext(
                method = "GET",
                uri = "https://klant.example.com/klantinteracties/api/v1/partijen?partijIdentificator__objectId=999993896",
                invokedMethod = null
            )

            `when`("the call is filtered") {
                val records = callFilter(clientRequestContext, createClientResponseContext(500))

                then("the record has the failure, no operation, and no part of the path or the query string") {
                    with(records.single()) {
                        mdc shouldContainAll mapOf(
                            "host" to "klant.example.com",
                            "operation" to "null",
                            "status" to "500",
                            "outcome" to "FAILURE"
                        )
                        containsAnywhere("999993896") shouldBe false
                        containsAnywhere("partijen") shouldBe false
                    }
                }
            }
        }

        given("FINE is enabled and a call that the remote system answers with a redirect") {
            val clientRequestContext = createClientRequestContext(
                method = "GET",
                uri = "https://example.com/fakePath",
                invokedMethod = null
            )

            `when`("the call is filtered") {
                val records = callFilter(clientRequestContext, createClientResponseContext(302))

                then("the call is logged as a success") {
                    with(records.single()) {
                        mdc["status"] shouldBe "302"
                        mdc["outcome"] shouldBe "SUCCESS"
                    }
                }
            }
        }

        given("FINE is enabled and a call without a URI, whose request was not given a correlation ID") {
            val clientRequestContext = mockk<ClientRequestContext>()
            every { clientRequestContext.method } returns "GET"
            every { clientRequestContext.uri } returns null
            every { clientRequestContext.getProperty(JsonLoggingFilter.START_TIME_PROPERTY) } returns System.nanoTime()
            every { clientRequestContext.getProperty(JsonLoggingFilter.CORRELATION_ID_PROPERTY) } returns null
            every { clientRequestContext.getProperty(JsonLoggingFilter.INVOKED_METHOD_PROPERTY) } returns null

            `when`("the response is filtered") {
                val records = LogCapture(JsonLoggingFilter::class, Level.FINE).use { logCapture ->
                    jsonLoggingFilter.filter(clientRequestContext, createClientResponseContext(200))
                    logCapture.records
                }

                then("the call is logged with a new correlation ID and without a host") {
                    with(records.single()) {
                        mdc.getValue("correlationId") shouldMatch Regex("[0-9a-f-]{36}")
                        mdc["host"] shouldBe "null"
                    }
                }
            }
        }

        given("FINE is enabled and a call inside an active span") {
            val spanContext = SpanContext.create(
                "0af7651916cd43dd8448eb211c80319c",
                "b7ad6b7169203331",
                TraceFlags.getSampled(),
                TraceState.getDefault()
            )
            val clientRequestContext = createClientRequestContext(
                method = "GET",
                uri = "https://example.com/fakePath",
                invokedMethod = null
            )

            `when`("the call is filtered") {
                val records = Span.wrap(spanContext).makeCurrent().use {
                    callFilter(clientRequestContext, createClientResponseContext(204))
                }

                then("the correlation ID is the span ID") {
                    records.single().mdc["correlationId"] shouldBe "b7ad6b7169203331"
                }
            }
        }

        given("FINE is disabled") {
            val clientRequestContext = mockk<ClientRequestContext>()
            every { clientRequestContext.getProperty(JsonLoggingFilter.START_TIME_PROPERTY) } returns null

            `when`("the call is filtered") {
                val records = callFilter(clientRequestContext, createClientResponseContext(200), Level.INFO)

                then("nothing is logged and nothing of the call is read") {
                    records.shouldBeEmpty()
                    verify(exactly = 0) { clientRequestContext.uri }
                }
            }
        }
    }
})
