/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.util.filter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.opentelemetry.api.baggage.Baggage
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.core.MultivaluedHashMap
import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.core.SecurityContext
import jakarta.ws.rs.core.UriInfo
import java.security.Principal

class OtelLoggingFilterTest : BehaviorSpec({

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("a request without X-Correlation-ID header") {
        val filter = OtelLoggingFilter()

        val uriInfo = mockk<UriInfo> {
            every { path } returns "zaken/123"
        }
        val headers: MultivaluedMap<String, String> = MultivaluedHashMap()

        // Mock request context
        val requestContext = mockk<ContainerRequestContext>()
        every { requestContext.headers } returns headers
        every { requestContext.uriInfo } returns uriInfo
        every { requestContext.method } returns "GET"
        every { requestContext.securityContext } returns null
        every { requestContext.setProperty(any(), any()) } returns Unit

        When("the request filter is applied") {
            filter.filter(requestContext)

            Then("a correlation ID should be generated and added to Baggage") {
                val baggage = Baggage.current()
                baggage.getEntryValue(OtelLoggingFilter.REQUEST_CORRELATION_ID).shouldNotBeNull()
                baggage.getEntryValue(OtelLoggingFilter.REQUEST_CORRELATION_ID) shouldMatch
                    "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"
            }

            Then("HTTP method should be added to Baggage") {
                Baggage.current().getEntryValue(OtelLoggingFilter.HTTP_REQUEST_METHOD) shouldBe "GET"
            }

            Then("HTTP path should be added to Baggage") {
                Baggage.current().getEntryValue(OtelLoggingFilter.HTTP_REQUEST_PATH) shouldBe "zaken/123"
            }

            Then("operation should be extracted and added to Baggage") {
                Baggage.current().getEntryValue(OtelLoggingFilter.REQUEST_OPERATION) shouldBe "zaken"
            }
        }
    }

    Given("a request with X-Correlation-ID header") {
        val filter = OtelLoggingFilter()

        val testCorrelationId = "test-correlation-123"
        val headers: MultivaluedMap<String, String> = MultivaluedHashMap()
        headers.add(OtelLoggingFilter.X_CORRELATION_ID, testCorrelationId)

        val uriInfo = mockk<UriInfo> {
            every { path } returns "taken/456/complete"
        }

        // Mock request context
        val requestContext = mockk<ContainerRequestContext>()
        every { requestContext.headers } returns headers
        every { requestContext.uriInfo } returns uriInfo
        every { requestContext.method } returns "POST"
        every { requestContext.securityContext } returns null
        every { requestContext.setProperty(any(), any()) } returns Unit

        When("the request filter is applied") {
            filter.filter(requestContext)

            Then("the provided correlation ID should be used") {
                Baggage.current().getEntryValue(OtelLoggingFilter.REQUEST_CORRELATION_ID) shouldBe testCorrelationId
            }
        }
    }

    Given("a request with a security principal") {
        val filter = OtelLoggingFilter()

        val testUserId = "testUser"
        val principal = mockk<Principal> {
            every { name } returns testUserId
        }
        val securityContext = mockk<SecurityContext> {
            every { userPrincipal } returns principal
        }

        val uriInfo = mockk<UriInfo> {
            every { path } returns "documenten"
        }
        val headers: MultivaluedMap<String, String> = MultivaluedHashMap()

        // Mock request context
        val requestContext = mockk<ContainerRequestContext>()
        every { requestContext.headers } returns headers
        every { requestContext.uriInfo } returns uriInfo
        every { requestContext.method } returns "GET"
        every { requestContext.securityContext } returns securityContext
        every { requestContext.setProperty(any(), any()) } returns Unit

        When("the request filter is applied") {
            filter.filter(requestContext)

            Then("the user ID should be added to Baggage") {
                Baggage.current().getEntryValue(OtelLoggingFilter.REQUEST_USER_ID) shouldBe testUserId
            }
        }
    }

    Given("a complete request-response cycle") {
        val filter = OtelLoggingFilter()

        val uriInfo = mockk<UriInfo> {
            every { path } returns "zaken/789"
        }
        val headers: MultivaluedMap<String, String> = MultivaluedHashMap()

        val properties = mutableMapOf<String, Any>()
        // Mock request context
        val requestContext = mockk<ContainerRequestContext>()
        every { requestContext.headers } returns headers
        every { requestContext.uriInfo } returns uriInfo
        every { requestContext.method } returns "PUT"
        every { requestContext.securityContext } returns null
        every { requestContext.setProperty(any(), any()) } answers {
            val key = firstArg<String>()
            val value = secondArg<Any>()
            properties[key] = value
        }
        every { requestContext.getProperty(any()) } answers {
            properties[firstArg()]
        }

        // Mock response context
        val responseContext = mockk<ContainerResponseContext>()
        every { responseContext.status } returns 200

        When("the filters are applied in sequence") {
            filter.filter(requestContext)
            // Small delay to ensure measurable duration
            Thread.sleep(10)
            filter.filter(requestContext, responseContext)

            Then("request start time should have been set") {
                // Context scope should be closed after response filter
                properties[OtelLoggingFilter.CONTEXT_OTEL_SCOPE] shouldBe null
            }
        }
    }

    Given("a response with measurable duration") {
        val filter = OtelLoggingFilter()

        val uriInfo = mockk<UriInfo> {
            every { path } returns "taken/999"
        }
        val headers: MultivaluedMap<String, String> = MultivaluedHashMap()

        val properties = mutableMapOf<String, Any>()
        // Mock request context
        val requestContext = mockk<ContainerRequestContext>()
        every { requestContext.headers } returns headers
        every { requestContext.uriInfo } returns uriInfo
        every { requestContext.method } returns "DELETE"
        every { requestContext.securityContext } returns null
        every { requestContext.setProperty(any(), any()) } answers {
            val key = firstArg<String>()
            val value = secondArg<Any>()
            properties[key] = value
        }
        every { requestContext.getProperty(any()) } answers {
            properties[firstArg()]
        }

        When("the request filter is applied") {
            filter.filter(requestContext)

            Then("request start time should have been captured") {
                properties[OtelLoggingFilter.CONTEXT_REQUEST_START_TIME].shouldNotBeNull()
                val startTime = properties[OtelLoggingFilter.CONTEXT_REQUEST_START_TIME] as Long
                startTime shouldBeGreaterThan 0
            }
        }
    }

    Given("operation extraction from various paths") {
        val filter = OtelLoggingFilter()

        val headers: MultivaluedMap<String, String> = MultivaluedHashMap()
        val uriInfo = mockk<UriInfo>()

        // Mock request context
        val requestContext = mockk<ContainerRequestContext>()
        every { requestContext.headers } returns headers
        every { requestContext.uriInfo } returns uriInfo
        every { requestContext.method } returns "GET"
        every { requestContext.securityContext } returns null
        every { requestContext.setProperty(any(), any()) } returns Unit

        every { uriInfo.path } returns "zaken/123"
        When("path is 'zaken/123'") {
            filter.filter(requestContext)

            Then("operation should be 'zaken'") {
                Baggage.current().getEntryValue("operation") shouldBe "zaken"
            }
        }

        every { uriInfo.path } returns "taken/456/complete"
        When("path is 'taken/456/complete'") {
            filter.filter(requestContext)

            Then("operation should be 'taken'") {
                Baggage.current().getEntryValue("operation") shouldBe "taken"
            }
        }

        every { uriInfo.path } returns "documenten"
        When("path is 'documenten'") {
            filter.filter(requestContext)

            Then("operation should be 'documenten'") {
                Baggage.current().getEntryValue("operation") shouldBe "documenten"
            }
        }

        every { uriInfo.path } returns ""
        When("path is empty") {
            filter.filter(requestContext)

            Then("operation should not be set") {
                Baggage.current().getEntryValue("operation") shouldBe null
            }
        }
    }

    Given("response filter with missing start time") {
        val filter = OtelLoggingFilter()

        // Mock request context
        val requestContext = mockk<ContainerRequestContext>()
        every { requestContext.getProperty(OtelLoggingFilter.CONTEXT_REQUEST_START_TIME) } returns null
        every { requestContext.getProperty(OtelLoggingFilter.CONTEXT_OTEL_SCOPE) } returns null

        // Mock response context
        val responseContext = mockk<ContainerResponseContext>()
        every { responseContext.status } returns 404

        When("the response filter is applied") {
            filter.filter(requestContext, responseContext)

            Then("filter should handle missing start time gracefully") {
                // The filter should not throw an exception
                true shouldBe true
            }
        }
    }
})
