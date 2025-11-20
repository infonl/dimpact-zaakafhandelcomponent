/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.util.filter

import jakarta.annotation.Priority
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import jakarta.ws.rs.ext.Provider
import org.jboss.logging.LoggerProvider
import org.jboss.logging.MDC
import java.security.Principal
import java.util.UUID
import java.util.logging.Logger

private const val HIGH_PRIORITY = 1000

/**
 * JAX-RS filter that automatically adds contextual information to MDC for all REST API requests.
 *
 * This filter enriches all log statements within a request with structured context including:
 * - requestId: Unique identifier for correlating all logs from a single request and/or across services
 * - userId: Current authenticated user (if available)
 * - httpMethod: HTTP method (GET, POST, PUT, DELETE, etc.)
 * - httpPath: Request path
 * - operation: First path segment (e.g., "zaken", "taken", "documenten")
 * - httpStatus: Response status code (added on response)
 * - durationMs: Request duration in milliseconds (added on response)
 *
 * The MDC context is automatically available to all log statements in the request thread,
 * including nested service calls, without requiring any code changes to existing business logic.
 *
 * Example log output:
 * ```json
 * {
 *   "@timestamp": "2025-01-06T14:00:00.123Z",
 *   "level": "INFO",
 *   "logger": "nl.info.zac.app.zaak.ZaakRestService",
 *   "message": "Zaak retrieved successfully",
 *   "mdc": {
 *     "correlationId": "550e8400-e29b-41d4-a716-446655440000",
 *     "userId": "johndoe",
 *     "httpMethod": "GET",
 *     "httpPath": "zaken/ZAAK-2024-0001",
 *     "operation": "zaken",
 *     "httpStatus": "200",
 *     "durationMs": "145"
 *   }
 * }
 * ```
 */
@Provider
@Priority(HIGH_PRIORITY) // Run early in the filter chain to ensure MDC is set before business logic
@Suppress("TooManyFunctions")
class MdcLoggingFilter(
    val loggerProvider: LoggerProvider? = null // Provided for testing purposes
) : ContainerRequestFilter, ContainerResponseFilter {
    companion object {
        private val LOG = Logger.getLogger(MdcLoggingFilter::class.java.name)

        // Property keys for passing data between request and response filters
        const val X_CORRELATION_ID = "X-Correlation-ID"
        const val CONTEXT_REQUEST_START_TIME = "context.request.startTime"
        const val MDC_OPERATION = "operation"
        const val MDC_CORRELATION_ID = "correlationId"
        const val MDC_HTTP_METHOD = "httpMethod"
        const val MDC_HTTP_PATH = "httpPath"
        const val MDC_HTTP_STATUS = "httpStatus"
        const val MDC_USER_ID = "userId"
        const val MDC_DURATION_MS = "durationMs"

        private val mdcKeys = setOf(
            MDC_CORRELATION_ID,
            MDC_HTTP_METHOD,
            MDC_HTTP_PATH,
            MDC_HTTP_STATUS,
            MDC_USER_ID,
            MDC_OPERATION,
            MDC_DURATION_MS
        )

        // Threshold for logging slow requests (in milliseconds)
        private const val SLOW_REQUEST_THRESHOLD_MS = 1000
    }

    /**
     * Called when a request arrives, before it reaches the REST resource method.
     * Sets up MDC context that will be available throughout the request processing.
     */
    override fun filter(requestContext: ContainerRequestContext) {
        // Store the request start time for calculating duration later
        requestContext.setRequestStartTime()

        // Add correlation ID from the header or generate a new one and set to MDC
        putMdc(MDC_CORRELATION_ID, requestContext.getOrNewCorrelationId())
        // Add request method, path, and operation to MDC
        putMdc(MDC_HTTP_METHOD, requestContext.method)
        putMdc(MDC_HTTP_PATH, requestContext.uriInfo.path)
        putMdc(MDC_OPERATION, extractOperationFromPath(requestContext.uriInfo.path))
        // Add authenticated user context if available to MDC
        putMdc(MDC_USER_ID, requestContext.getUserPrincipal()?.name)

        // Examples: "zaken/123" -> "zaken", "taken/456/complete" -> "taken"
        LOG.fine("Request started: ${requestContext.method} ${requestContext.uriInfo.path}")
    }

    /**
     * Called after the REST resource method has been executed, before sending the response.
     * Adds response metadata to MDC and cleans up all MDC keys.
     */
    override fun filter(
        requestContext: ContainerRequestContext,
        responseContext: ContainerResponseContext
    ) {
        try {
            // Add response status to MDC
            val status = responseContext.status
            putMdc(MDC_HTTP_STATUS, status.toString())

            // Calculate and add request duration
            requestContext.getRequestStartTime()?.run {
                val duration = System.currentTimeMillis() - this
                putMdc(MDC_DURATION_MS, duration.toString())

                // Log slow requests as warnings for monitoring
                if (duration > SLOW_REQUEST_THRESHOLD_MS) {
                    LOG.warning(
                        "Slow request detected: ${requestContext.method} ${requestContext.uriInfo.path} " +
                            "took ${duration}ms (threshold: ${SLOW_REQUEST_THRESHOLD_MS}ms)"
                    )
                }
                LOG.fine("Request completed: status=$status, duration=${duration}ms")
            }
        } finally {
            // Clean up all MDC keys that were set during request processing
            // This is critical to prevent memory leaks and context pollution between requests
            mdcKeys.forEach(::removeMdc)
        }
    }

    private fun removeMdc(key: String) {
        if (loggerProvider != null) {
            loggerProvider.removeMdc(key)
        } else {
            MDC.remove(key)
        }
    }

    /**
     * Helper to put a key-value pair into MDC if the value is not null.
     */
    private fun putMdc(key: String, value: String?) {
        if (value != null) {
            if (loggerProvider != null) {
                loggerProvider.putMdc(key, value)
            } else {
                MDC.put(key, value)
            }
        }
    }

    /**
     * Extract operation name from the request path.
     * Returns the first non-empty path segment.
     *
     * Examples:
     * - "zaken/123" -> "zaken"
     * - "taken/456/complete" -> "taken"
     * - "documenten" -> "documenten"
     * - "" -> null
     *
     * @param path Request path
     * @return First path segment or null if path is empty
     */
    private fun extractOperationFromPath(path: String): String? =
        path.split("/")
            .firstOrNull { it.isNotBlank() }

    // Helper extension functions for ContainerRequestContext
    private fun ContainerRequestContext.getUserPrincipal(): Principal? = securityContext?.userPrincipal
    private fun ContainerRequestContext.setRequestStartTime() {
        setProperty(CONTEXT_REQUEST_START_TIME, System.currentTimeMillis())
    }
    private fun ContainerRequestContext.getRequestStartTime(): Long? = getProperty(CONTEXT_REQUEST_START_TIME) as? Long
    private fun ContainerRequestContext.getOrNewCorrelationId(): String =
        this.headers[X_CORRELATION_ID]?.firstOrNull() ?: UUID.randomUUID().toString()
}
