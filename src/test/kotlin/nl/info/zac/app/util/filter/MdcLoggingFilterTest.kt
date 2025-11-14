/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.util.filter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldMatch
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.core.MultivaluedHashMap
import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.core.SecurityContext
import jakarta.ws.rs.core.UriInfo
import org.jboss.logging.MDC
import java.security.Principal

class MdcLoggingFilterTest : BehaviorSpec({

    lateinit var filter: MdcLoggingFilter
    lateinit var httpServletRequest: HttpServletRequest
    
    beforeEach {
        checkUnnecessaryStub()
        // Clear MDC before each test
        MDC.clear()
        // Create new instances for each test
        httpServletRequest = mockk<HttpServletRequest>()
        filter = MdcLoggingFilter()
        // Inject the mock HttpServletRequest using reflection
        filter::class.java.getDeclaredField("httpServletRequest").apply {
            isAccessible = true
            set(filter, httpServletRequest)
        }
    }

    afterSpec {
        unmockkStatic(MDC::class)
    }

    Given("a request without x_request_id header") {
        val requestContext = mockk<ContainerRequestContext>()
        val uriInfo = mockk<UriInfo>()
        val headers: MultivaluedMap<String, String> = MultivaluedHashMap()

        every { requestContext.headers } returns headers
        every { requestContext.uriInfo } returns uriInfo
        every { requestContext.method } returns "GET"
        every { uriInfo.path } returns "zaken/123"
        every { requestContext.securityContext } returns null
        every { requestContext.setProperty(any(), any()) } returns Unit
        every { httpServletRequest.getSession(false) } returns null

        When("the request filter is applied") {
            filter.filter(requestContext)

            Then("a new requestId should be generated and stored") {
                verify { requestContext.setProperty("mdc.requestId", match { it is String }) }
            }

            Then("requestId should be added to MDC") {
                val requestId = MDC.get("requestId")
                requestId.shouldNotBeNull()
                requestId.toString() shouldMatch "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}".toRegex()
            }

            Then("httpMethod should be added to MDC") {
                MDC.get("httpMethod").toString() shouldBe "GET"
            }

            Then("httpPath should be added to MDC") {
                MDC.get("httpPath").toString() shouldBe "zaken/123"
            }

            Then("operation should be extracted from path") {
                MDC.get("operation").toString() shouldBe "zaken"
            }

            Then("start time should be stored in request context") {
                verify { requestContext.setProperty("mdc.request.startTime", match { it is Long }) }
            }
        }
    }

    Given("a request with x_request_id header") {
        val requestContext = mockk<ContainerRequestContext>()
        val uriInfo = mockk<UriInfo>()
        val headers: MultivaluedMap<String, String> = MultivaluedHashMap()
        val existingRequestId = "custom-request-id-123"
        headers.add("x_request_id", existingRequestId)

        every { requestContext.headers } returns headers
        every { requestContext.uriInfo } returns uriInfo
        every { requestContext.method } returns "POST"
        every { uriInfo.path } returns "taken/456/complete"
        every { requestContext.securityContext } returns null
        every { requestContext.setProperty(any(), any()) } returns Unit
        every { httpServletRequest.getSession(false) } returns null

        When("the request filter is applied") {
            filter.filter(requestContext)

            Then("the existing requestId should be used") {
                verify { requestContext.setProperty("mdc.requestId", existingRequestId) }
            }

            Then("requestId in MDC should match the header value") {
                MDC.get("requestId").toString() shouldBe existingRequestId
            }
        }
    }

    Given("a request with authenticated user") {
        val requestContext = mockk<ContainerRequestContext>()
        val uriInfo = mockk<UriInfo>()
        val securityContext = mockk<SecurityContext>()
        val principal = mockk<Principal>()
        val headers: MultivaluedMap<String, String> = MultivaluedHashMap()

        every { requestContext.headers } returns headers
        every { requestContext.uriInfo } returns uriInfo
        every { requestContext.method } returns "GET"
        every { uriInfo.path } returns "documenten"
        every { requestContext.securityContext } returns securityContext
        every { securityContext.userPrincipal } returns principal
        every { principal.name } returns "john.doe"
        every { requestContext.setProperty(any(), any()) } returns Unit
        every { httpServletRequest.getSession(false) } returns null

        When("the request filter is applied") {
            filter.filter(requestContext)

            Then("userId should be added to MDC") {
                MDC.get("userId").toString() shouldBe "john.doe"
            }
        }
    }

    Given("a request with X-Correlation-ID header") {
        val requestContext = mockk<ContainerRequestContext>()
        val uriInfo = mockk<UriInfo>()
        val headers: MultivaluedMap<String, String> = MultivaluedHashMap()
        val correlationId = "correlation-123"
        headers.add("X-Correlation-ID", correlationId)

        every { requestContext.headers } returns headers
        every { requestContext.uriInfo } returns uriInfo
        every { requestContext.method } returns "PUT"
        every { uriInfo.path } returns "settings/update"
        every { requestContext.securityContext } returns null
        every { requestContext.setProperty(any(), any()) } returns Unit
        every { httpServletRequest.getSession(false) } returns null

        When("the request filter is applied") {
            filter.filter(requestContext)

            Then("correlationId should be added to MDC") {
                MDC.get("correlationId").toString() shouldBe correlationId
            }
        }
    }

    Given("a completed request") {
        val requestContext = mockk<ContainerRequestContext>()
        val responseContext = mockk<ContainerResponseContext>()
        val uriInfo = mockk<UriInfo>()
        val mdcKeys = mutableSetOf("requestId", "httpMethod", "httpPath", "operation")
        val startTime = System.currentTimeMillis() - 150 // Simulate 150ms request

        every { requestContext.getProperty("mdc.keys") } returns mdcKeys
        every { requestContext.getProperty("mdc.request.startTime") } returns startTime
        every { responseContext.status } returns 200
        every { requestContext.method } returns "GET"
        every { requestContext.uriInfo } returns uriInfo
        every { uriInfo.path } returns "zaken/123"

        When("the response filter is applied") {
            filter.filter(requestContext, responseContext)

            Then("httpStatus should be added to MDC") {
                MDC.get("httpStatus").toString() shouldBe "200"
            }

            Then("durationMs should be calculated and added to MDC") {
                val duration = MDC.get("durationMs").toString().toLong()
                duration shouldBe 150
            }

            Then("all MDC keys should be cleaned up") {
                MDC.get("requestId") shouldBe null
                MDC.get("httpMethod") shouldBe null
                MDC.get("httpPath") shouldBe null
                MDC.get("operation") shouldBe null
                MDC.get("httpStatus") shouldBe null
                MDC.get("durationMs") shouldBe null
            }
        }
    }

    Given("a slow request exceeding threshold") {
        val requestContext = mockk<ContainerRequestContext>()
        val responseContext = mockk<ContainerResponseContext>()
        val uriInfo = mockk<UriInfo>()
        val mdcKeys = mutableSetOf("requestId", "httpMethod", "httpPath")
        val startTime = System.currentTimeMillis() - 1500 // Simulate 1500ms request (slow)

        every { requestContext.getProperty("mdc.keys") } returns mdcKeys
        every { requestContext.getProperty("mdc.request.startTime") } returns startTime
        every { responseContext.status } returns 200
        every { requestContext.method } returns "POST"
        every { requestContext.uriInfo } returns uriInfo
        every { uriInfo.path } returns "taken/456"

        When("the response filter is applied") {
            filter.filter(requestContext, responseContext)

            Then("duration should exceed threshold") {
                val duration = MDC.get("durationMs").toString().toLong()
                duration shouldBe 1500
            }

            // Note: Verifying the warning log would require additional logging framework setup
            // This is a candidate for integration testing
        }
    }

    Given("different path patterns") {
        val requestContext = mockk<ContainerRequestContext>()
        val uriInfo = mockk<UriInfo>()
        val headers: MultivaluedMap<String, String> = MultivaluedHashMap()

        every { requestContext.headers } returns headers
        every { requestContext.uriInfo } returns uriInfo
        every { requestContext.method } returns "GET"
        every { requestContext.securityContext } returns null
        every { requestContext.setProperty(any(), any()) } returns Unit
        every { httpServletRequest.getSession(false) } returns null

        When("path is 'zaken/123'") {
            every { uriInfo.path } returns "zaken/123"
            filter.filter(requestContext)
            MDC.clear()

            Then("operation should be 'zaken'") {
                every { uriInfo.path } returns "zaken/123"
                filter.filter(requestContext)
                MDC.get("operation").toString() shouldBe "zaken"
            }
        }

        When("path is 'taken/456/complete'") {
            MDC.clear()
            every { uriInfo.path } returns "taken/456/complete"
            filter.filter(requestContext)

            Then("operation should be 'taken'") {
                MDC.get("operation").toString() shouldBe "taken"
            }
        }

        When("path is 'documenten'") {
            MDC.clear()
            every { uriInfo.path } returns "documenten"
            filter.filter(requestContext)

            Then("operation should be 'documenten'") {
                MDC.get("operation").toString() shouldBe "documenten"
            }
        }

        When("path is empty") {
            MDC.clear()
            every { uriInfo.path } returns ""
            filter.filter(requestContext)

            Then("operation should not be set") {
                MDC.get("operation") shouldBe null
            }
        }
    }

    Given("response filter with missing start time") {
        val requestContext = mockk<ContainerRequestContext>()
        val responseContext = mockk<ContainerResponseContext>()
        val mdcKeys = mutableSetOf("requestId")

        every { requestContext.getProperty("mdc.keys") } returns mdcKeys
        every { requestContext.getProperty("mdc.request.startTime") } returns null
        every { responseContext.status } returns 404

        When("the response filter is applied") {
            filter.filter(requestContext, responseContext)

            Then("httpStatus should still be added") {
                MDC.get("httpStatus").toString() shouldBe "404"
            }

            Then("durationMs should not be set") {
                MDC.get("durationMs") shouldBe null
            }

            Then("MDC should be cleaned up") {
                MDC.get("requestId") shouldBe null
                MDC.get("httpStatus") shouldBe null
            }
        }
    }

    Given("a request with HTTP session") {
        val requestContext = mockk<ContainerRequestContext>()
        val uriInfo = mockk<UriInfo>()
        val httpSession = mockk<HttpSession>()
        val headers: MultivaluedMap<String, String> = MultivaluedHashMap()
        val sessionId = "A1B2C3D4E5F6G7H8"

        every { requestContext.headers } returns headers
        every { requestContext.uriInfo } returns uriInfo
        every { requestContext.method } returns "POST"
        every { uriInfo.path } returns "taken/create"
        every { requestContext.securityContext } returns null
        every { requestContext.setProperty(any(), any()) } returns Unit
        every { httpServletRequest.getSession(false) } returns httpSession
        every { httpSession.id } returns sessionId

        When("the request filter is applied") {
            filter.filter(requestContext)

            Then("sessionId should be added to MDC") {
                MDC.get("sessionId").toString() shouldBe sessionId
            }
        }
    }

    Given("a request without HTTP session") {
        val requestContext = mockk<ContainerRequestContext>()
        val uriInfo = mockk<UriInfo>()
        val headers: MultivaluedMap<String, String> = MultivaluedHashMap()

        every { requestContext.headers } returns headers
        every { requestContext.uriInfo } returns uriInfo
        every { requestContext.method } returns "GET"
        every { uriInfo.path } returns "health"
        every { requestContext.securityContext } returns null
        every { requestContext.setProperty(any(), any()) } returns Unit
        every { httpServletRequest.getSession(false) } returns null

        When("the request filter is applied") {
            filter.filter(requestContext)

            Then("sessionId should not be added to MDC") {
                MDC.get("sessionId") shouldBe null
            }
        }
    }
})
