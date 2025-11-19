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
import io.mockk.verify
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.core.MultivaluedHashMap
import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.core.SecurityContext
import jakarta.ws.rs.core.UriInfo
import org.jboss.logging.Logger
import org.jboss.logging.LoggerProvider
import java.security.Principal

class MdcLoggingFilterTest : BehaviorSpec({

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("a request without X-Correlation-ID header") {
        // Build a fresh filter and logger provider for each test case
        val loggerProvider = TestLoggerProvider()
        var filter = MdcLoggingFilter(loggerProvider)

        val uriInfo = mockk<UriInfo>{
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

            Then("Correlation-Id should be added to MDC") {
                val correlationId = loggerProvider.getMdc(MdcLoggingFilter.MDC_CORRELATION_ID)
                correlationId.shouldNotBeNull()
                correlationId.toString() shouldMatch "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}".toRegex()
            }

            And("httpMethod should be added to MDC") {
                loggerProvider.getMdc(MdcLoggingFilter.MDC_HTTP_METHOD).toString() shouldBe "GET"
            }

            And("httpPath should be added to MDC") {
                loggerProvider.getMdc("httpPath").toString() shouldBe "zaken/123"
            }

            And("operation should be extracted from path") {
                loggerProvider.getMdc("operation").toString() shouldBe "zaken"
            }

            And("start time should be stored in request context") {
                verify { requestContext.setProperty(MdcLoggingFilter.CONTEXT_REQUEST_START_TIME, match { it is Long }) }
            }
        }
    }

    Given("a request with x_request_id header") {
        // Build a fresh filter and logger provider for each test case
        val loggerProvider = TestLoggerProvider()
        var filter = MdcLoggingFilter(loggerProvider)

        val existingCorrelationId = "correlation-id-123"
        // Mock request context
        val requestContext = mockk<ContainerRequestContext>()
        every { requestContext.headers } returns MultivaluedHashMap(
            mapOf(Pair(MdcLoggingFilter.X_CORRELATION_ID, existingCorrelationId))
        )
        every { requestContext.uriInfo } returns mockk<UriInfo> {
            every { path } returns "taken/456/complete"
        }
        every { requestContext.method } returns "POST"
        every { requestContext.securityContext } returns null
        every { requestContext.setProperty(any(), any()) } returns Unit

        When("the request filter is applied") {
            filter.filter(requestContext)

            Then("correlation id in MDC should match the header value") {
                loggerProvider.getMdc(MdcLoggingFilter.MDC_CORRELATION_ID).toString() shouldBe existingCorrelationId
            }
        }
    }

    Given("a request with authenticated user") {
        // Build a fresh filter and logger provider for each test case
        val loggerProvider = TestLoggerProvider()
        var filter = MdcLoggingFilter(loggerProvider)

        // Mock request context
        val requestContext = mockk<ContainerRequestContext>()
        every { requestContext.headers } returns MultivaluedHashMap()
        every { requestContext.uriInfo } returns mockk<UriInfo> {
            every<String> { path } returns "documenten"
        }
        every { requestContext.method } returns "GET"
        every { requestContext.securityContext } returns mockk<SecurityContext> {
            every<Principal> { userPrincipal } returns mockk<Principal> {
                every<String> { name } returns "john.doe"
            }
        }
        every { requestContext.setProperty(any(), any()) } returns Unit

        When("the request filter is applied") {
            filter.filter(requestContext)

            Then("userId should be added to MDC") {
                loggerProvider.getMdc(MdcLoggingFilter.MDC_USER_ID).toString() shouldBe "john.doe"
            }
        }
    }

    Given("a request with X-Correlation-ID header") {
        // Build a fresh filter and logger provider for each test case
        val loggerProvider = TestLoggerProvider()
        var filter = MdcLoggingFilter(loggerProvider)

        val correlationId = "correlation-123"
        // Mock request context
        val requestContext = mockk<ContainerRequestContext>()
        every { requestContext.headers } returns MultivaluedHashMap(
            mapOf(Pair("X-Correlation-ID", correlationId))
        )
        every { requestContext.uriInfo } returns mockk<UriInfo> {
            every<String> { path } returns "settings/update"
        }
        every { requestContext.method } returns "PUT"
        every { requestContext.securityContext } returns null
        every { requestContext.setProperty(any(), any()) } returns Unit

        When("the request filter is applied") {
            filter.filter(requestContext)

            Then("correlationId should be added to MDC") {
                loggerProvider.getMdc(MdcLoggingFilter.MDC_CORRELATION_ID).toString() shouldBe correlationId
            }
        }
    }

    Given("a completed request") {
        // Build a fresh filter and logger provider for each test case
        val loggerProvider = TestLoggerProvider()
        var filter = MdcLoggingFilter(loggerProvider)

        // Mock request contexts
        val requestContext = mockk<ContainerRequestContext>()
        every { requestContext.getProperty(MdcLoggingFilter.CONTEXT_REQUEST_START_TIME) } returns
                System.currentTimeMillis() - 150

        // Mock response context
        val responseContext = mockk<ContainerResponseContext>()
        every { responseContext.status } returns 200

        When("the response filter is applied") {
            filter.filter(requestContext, responseContext)

            Then("httpStatus should be added to MDC") {
                loggerProvider.removedMdc[MdcLoggingFilter.MDC_HTTP_STATUS] shouldBe "200"
            }

            Then("durationMs should be calculated and added to MDC") {
                val duration = loggerProvider.removedMdc[MdcLoggingFilter.MDC_DURATION_MS].toString().toLong()
                duration shouldBeGreaterThan 150
            }
        }
    }

    Given("a slow request exceeding threshold") {
        // Build a fresh filter and logger provider for each test case
        val loggerProvider = TestLoggerProvider()
        var filter = MdcLoggingFilter(loggerProvider)

        // Mock request contexts
        val requestContext = mockk<ContainerRequestContext>()
        every { requestContext.getProperty(MdcLoggingFilter.CONTEXT_REQUEST_START_TIME) } returns
                System.currentTimeMillis() - 1500
        every { requestContext.method } returns "POST"
        every { requestContext.uriInfo } returns mockk<UriInfo> {
            every<String> { path } returns "taken/456"
        }

        // Mock response context
        val responseContext = mockk<ContainerResponseContext>()
        every { responseContext.status } returns 200

        When("the response filter is applied") {
            filter.filter(requestContext, responseContext)

            Then("duration should exceed threshold") {
                val duration = loggerProvider.removedMdc[MdcLoggingFilter.MDC_DURATION_MS].toString().toLong()
                duration shouldBeGreaterThan 1500
            }
        }
    }

    Given("different path patterns") {
        // Build a fresh filter and logger provider for each test case
        val loggerProvider = TestLoggerProvider()
        var filter = MdcLoggingFilter(loggerProvider)

        // Mock request context
        val requestContext = mockk<ContainerRequestContext>()
        every { requestContext.headers } returns MultivaluedHashMap()
        val uriInfo = mockk<UriInfo>()
        every { requestContext.uriInfo } returns uriInfo
        every { requestContext.method } returns "GET"
        every { requestContext.securityContext } returns null
        every { requestContext.setProperty(any(), any()) } returns Unit

        every { uriInfo.path } returns "zaken/123"
        When("path is 'zaken/123'") {
            loggerProvider.clearMdc()
            filter.filter(requestContext)

            Then("operation should be 'zaken'") {
                loggerProvider.getMdc("operation").toString() shouldBe "zaken"
            }
        }

        every { uriInfo.path } returns "taken/456/complete"
        When("path is 'taken/456/complete'") {
            loggerProvider.clearMdc()
            filter.filter(requestContext)

            Then("operation should be 'taken'") {
                loggerProvider.getMdc("operation").toString() shouldBe "taken"
            }
        }

        every { uriInfo.path } returns "documenten"
        When("path is 'documenten'") {
            loggerProvider.clearMdc()
            filter.filter(requestContext)

            Then("operation should be 'documenten'") {
                loggerProvider.getMdc("operation").toString() shouldBe "documenten"
            }
        }

        every { uriInfo.path } returns ""
        When("path is empty") {
            loggerProvider.clearMdc()
            filter.filter(requestContext)

            Then("operation should not be set") {
                loggerProvider.getMdc("operation") shouldBe null
            }
        }
    }

    Given("response filter with missing start time") {
        // Build a fresh filter and logger provider for each test case
        val loggerProvider = TestLoggerProvider()
        var filter = MdcLoggingFilter(loggerProvider)

        // Mock request context
        val requestContext = mockk<ContainerRequestContext>()
        every { requestContext.getProperty(MdcLoggingFilter.CONTEXT_REQUEST_START_TIME) } returns null

        // Mock response context
        val responseContext = mockk<ContainerResponseContext>()
        every { responseContext.status } returns 404

        When("the response filter is applied") {
            filter.filter(requestContext, responseContext)

            Then("httpStatus should still be added") {
                loggerProvider.removedMdc[MdcLoggingFilter.MDC_HTTP_STATUS] shouldBe "404"
            }

            Then("durationMs should not be set") {
                loggerProvider.removedMdc[MdcLoggingFilter.MDC_DURATION_MS] shouldBe null
            }
        }
    }
})

class TestLoggerProvider : LoggerProvider {
    private val loggers = mutableMapOf<String, Logger>()
    val mdc = mutableMapOf<String, Any?>()
    val removedMdc = mutableMapOf<String, Any?>()

    override fun getLogger(name: String?): Logger {
        return loggers.getOrPut(name ?: "default") {
            Logger.getLogger(name)
        }
    }

    // MDC methods for testing
    override fun clearMdc() = mdc.clear()
    override fun putMdc(key: String, value: Any?) = mdc.put(key, value)
    override fun getMdc(key: String) = mdc[key]
    override fun removeMdc(key: String) {
        removedMdc[key] = mdc[key]
        mdc.remove(key)
    }
    override fun getMdcMap(): Map<String, Any?> = mdc.toMap()
    override fun clearNdc() = mdc.clear()

    // No NDC functionality implemented for this test provider
    override fun getNdc(): String? = null
    override fun getNdcDepth(): Int = 0
    override fun popNdc(): String? = null
    override fun peekNdc(): String? = null
    override fun pushNdc(key: String?) {}
    override fun setNdcMaxDepth(depth: Int) {}
}
