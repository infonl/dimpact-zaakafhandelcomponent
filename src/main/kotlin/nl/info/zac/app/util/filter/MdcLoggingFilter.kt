/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.util.filter

import jakarta.annotation.Priority
import jakarta.servlet.http.HttpServletRequest
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.ext.Provider
import org.jboss.logging.MDC
import java.util.UUID
import java.util.logging.Logger

private const val HIGH_PRIORITY = 1000

/**
 * JAX-RS filter that automatically adds contextual information to MDC for all REST API requests.
 *
 * This filter enriches all log statements within a request with structured context including:
 * - requestId: Unique identifier for correlating all logs from a single request and/or across services
 * - sessionId: HTTP session ID for correlating multiple requests from the same user session
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
 *     "requestId": "550e8400-e29b-41d4-a716-446655440000",
 *     "sessionId": "B8F5C2D3A1E9F7C6",
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
class MdcLoggingFilter : ContainerRequestFilter, ContainerResponseFilter {
    @Context
    private var httpServletRequest: HttpServletRequest? = null
    companion object {
        private val LOG = Logger.getLogger(MdcLoggingFilter::class.java.name)

        // Property keys for passing data between request and response filters
        private const val REQUEST_START_TIME = "mdc.request.startTime"
        private const val X_REQUEST_ID = "x_request_id"
        private const val REQUEST_ID = "mdc.requestId"
        private const val MDC_KEYS = "mdc.keys"

        // Threshold for logging slow requests (in milliseconds)
        private const val SLOW_REQUEST_THRESHOLD_MS = HIGH_PRIORITY
    }

    /**
     * Called when a request arrives, before it reaches the REST resource method.
     * Sets up MDC context that will be available throughout the request processing.
     */
    override fun filter(requestContext: ContainerRequestContext) {
        val mdcKeys = mutableSetOf<String>()

        // Get current request ID, or  for correlation
        val requestId = requestContext.headers[X_REQUEST_ID]?.firstOrNull() ?: UUID.randomUUID().toString()
        requestContext.setProperty(REQUEST_ID, requestId)
        requestContext.setProperty(REQUEST_START_TIME, System.currentTimeMillis())

        // Add request context to MDC
        putMDC("requestId", requestId, mdcKeys)
        putMDC("httpMethod", requestContext.method, mdcKeys)
        putMDC("httpPath", requestContext.uriInfo.path, mdcKeys)

        // Add HTTP session ID for correlating multiple requests from same session
        // Don't create a new session if one doesn't exist (getSession(false))
        httpServletRequest?.getSession(false)?.id?.let { sessionId ->
            putMDC("sessionId", sessionId, mdcKeys)
        }

        // Add authenticated user context if available
        requestContext.securityContext?.userPrincipal?.let { principal ->
            putMDC("userId", principal.name, mdcKeys)
        }

        // Extract operation name from path (first path segment)
        // Examples: "zaken/123" -> "zaken", "taken/456/complete" -> "taken"
        extractOperationFromPath(requestContext.uriInfo.path)?.let { operation ->
            putMDC("operation", operation, mdcKeys)
        }

        // Add correlation ID if present in headers (for distributed tracing)
        requestContext.headers["X-Correlation-ID"]?.firstOrNull()?.let { correlationId ->
            putMDC("correlationId", correlationId, mdcKeys)
        }

        // Store MDC keys for cleanup
        requestContext.setProperty(MDC_KEYS, mdcKeys)

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
            @Suppress("UNCHECKED_CAST")
            val mdcKeys = requestContext.getProperty(MDC_KEYS) as? MutableSet<String>

            // Add response status to MDC
            val status = responseContext.status
            putMDC("httpStatus", status.toString(), mdcKeys)

            // Calculate and add request duration
            val startTime = requestContext.getProperty(REQUEST_START_TIME) as? Long
            startTime?.let {
                val duration = System.currentTimeMillis() - it
                putMDC("durationMs", duration.toString(), mdcKeys)

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
            @Suppress("UNCHECKED_CAST")
            val mdcKeys = requestContext.getProperty(MDC_KEYS) as? Set<String>
            mdcKeys?.forEach { key ->
                MDC.remove(key)
            }
        }
    }

    /**
     * Helper method to put a value into MDC and track the key for later cleanup.
     *
     * @param key MDC key
     * @param value MDC value
     * @param tracker Set to track keys for cleanup (optional)
     */
    private fun putMDC(key: String, value: String, tracker: MutableSet<String>?) {
        MDC.put(key, value)
        tracker?.add(key)
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
}
