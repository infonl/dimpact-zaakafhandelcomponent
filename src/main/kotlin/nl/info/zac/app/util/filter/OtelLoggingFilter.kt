/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.util.filter

import io.opentelemetry.api.baggage.Baggage
import io.opentelemetry.api.baggage.BaggageBuilder
import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Context
import io.opentelemetry.context.Scope
import jakarta.annotation.Priority
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import jakarta.ws.rs.ext.Provider
import java.security.Principal
import java.util.UUID
import java.util.logging.Logger

private const val HIGH_PRIORITY = 1000

/**
 * JAX-RS filter that automatically adds contextual information to OpenTelemetry Baggage for all REST API requests.
 *
 * This filter enriches all log statements within a request with structured context including:
 * - correlationId: Unique identifier for correlating all logs from a single request and/or across services
 * - userId: Current authenticated user (if available)
 * - httpMethod: HTTP method (GET, POST, PUT, DELETE, etc.)
 * - httpPath: Request path
 * - operation: First path segment (e.g., "zaken", "taken", "documenten")
 * - httpStatus: Response status code (added on response)
 * - durationMs: Request duration in milliseconds (added on response)
 *
 * The context is stored in OpenTelemetry Baggage and automatically propagates across thread boundaries
 * and service calls, making it available to all log statements without requiring code changes.
 *
 * Example log output:
 * ```json
 * {
 *   "@timestamp": "2025-01-06T14:00:00.123Z",
 *   "level": "INFO",
 *   "logger": "nl.info.zac.app.zaak.ZaakRestService",
 *   "message": "Zaak retrieved successfully",
 *   "baggage": {
 *     "correlationId": "550e8400-e29b-41d4-a716-446655440000",
 *     "traceId": "0af7651916cd43dd8448eb211c80319c",
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
@Priority(HIGH_PRIORITY) // Run early in the filter chain to ensure context is set before business logic
@Suppress("TooManyFunctions")
class OtelLoggingFilter : ContainerRequestFilter, ContainerResponseFilter {
    companion object {
        private val LOG = Logger.getLogger(OtelLoggingFilter::class.java.name)

        // Property keys for passing data between request and response filters
        const val X_CORRELATION_ID = "X-Correlation-ID"
        const val CONTEXT_REQUEST_START_TIME = "context.request.startTime"
        const val CONTEXT_OTEL_SCOPE = "context.otel.scope"
        const val OTEL_TRACE_ID = "traceId"
        const val REQUEST_OPERATION = "operation"
        const val REQUEST_CORRELATION_ID = "correlationId"
        const val REQUEST_USER_ID = "userId"
        const val HTTP_REQUEST_METHOD = "httpMethod"
        const val HTTP_REQUEST_PATH = "httpPath"
        const val HTTP_RESPONSE_STATUS = "httpStatus"
        const val PROCES_DURATION_MS = "durationMs"
    }

    /**
     * Called when a request arrives, before it reaches the REST resource method.
     * Sets up OpenTelemetry Baggage context that will be available throughout the request processing.
     */
    override fun filter(requestContext: ContainerRequestContext) {
        // Store the request start time for calculating duration later
        requestContext.setRequestStartTime()

        // Build baggage with request context
        val baggage = Baggage.current().toBuilder().run {
            putOrRemove(OTEL_TRACE_ID, getTraceId())
            putOrRemove(REQUEST_OPERATION, extractOperationFromPath(requestContext.uriInfo.path))
            putOrRemove(REQUEST_USER_ID, requestContext.getUserPrincipal()?.name)
            put(REQUEST_CORRELATION_ID, requestContext.getOrNewCorrelationId())
            put(HTTP_REQUEST_METHOD, requestContext.method)
            put(HTTP_REQUEST_PATH, requestContext.uriInfo.path)
        }.build()

        // Make the context current and store the scope for cleanup
        val scope = Context.current().with(baggage).makeCurrent()
        requestContext.setProperty(CONTEXT_OTEL_SCOPE, scope)

        LOG.fine("Request started: ${requestContext.method} ${requestContext.uriInfo.path}")
    }

    /**
     * Called after the REST resource method has been executed, before sending the response.
     * Adds response metadata to Baggage and cleans up the OpenTelemetry context.
     */
    @Suppress("NestedBlockDepth")
    override fun filter(
        requestContext: ContainerRequestContext,
        responseContext: ContainerResponseContext
    ) {
        try {
            // Add response status and duration to Baggage
            val status = responseContext.status
            var baggage = Baggage.current().toBuilder()
                .put(HTTP_RESPONSE_STATUS, status.toString())
                .build()

            val duration = requestContext.getRequestDuration()?.also {
                baggage = baggage.toBuilder().put(PROCES_DURATION_MS, it.toString()).build()
            }

            // Update context with response metadata
            Context.current().with(baggage).makeCurrent().use {
                LOG.fine("Request completed: status=$status, duration=${duration ?: "??"} ms")
            }
        } finally {
            // Clean up OpenTelemetry context scope to prevent memory leaks
            (requestContext.getProperty(CONTEXT_OTEL_SCOPE) as? Scope)?.close()
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

    /**
     * Returns the current OpenTelemetry trace ID if a valid span is active, or null otherwise.
     */
    private fun getTraceId(): String? =
        Span.current().spanContext
            .takeIf { it.isValid }
            ?.traceId

    private fun BaggageBuilder.putOrRemove(key: String, value: String?) {
        if (value == null) remove(key)
        else put(key, value)
    }

    // Helper extension functions for ContainerRequestContext
    private fun ContainerRequestContext.getUserPrincipal(): Principal? = securityContext?.userPrincipal
    private fun ContainerRequestContext.setRequestStartTime() {
        setProperty(CONTEXT_REQUEST_START_TIME, System.currentTimeMillis())
    }
    private fun ContainerRequestContext.getRequestDuration() = if (hasProperty(CONTEXT_REQUEST_START_TIME)) {
        System.currentTimeMillis() - (getProperty(CONTEXT_REQUEST_START_TIME) as Long)
    } else {
        null
    }
    private fun ContainerRequestContext.getOrNewCorrelationId(): String =
        this.headers[X_CORRELATION_ID]?.firstOrNull() ?: getTraceId() ?: UUID.randomUUID().toString()
}
