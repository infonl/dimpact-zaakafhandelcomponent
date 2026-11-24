/*
 * SPDX-FileCopyrightText: 2021 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util

import jakarta.ws.rs.client.ClientRequestContext
import jakarta.ws.rs.client.ClientRequestFilter
import jakarta.ws.rs.client.ClientResponseContext
import jakarta.ws.rs.client.ClientResponseFilter
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import nl.info.zac.log.Outcome
import nl.info.zac.log.ZacLogger
import nl.info.zac.util.log.OutboundRestCall
import java.lang.reflect.Method
import java.util.concurrent.TimeUnit
import java.util.logging.Level

/**
 * Logs one record per outbound REST call that gets a response, without its path, query string, headers or payload,
 * because those can hold personal data.
 */
@Provider
class JsonLoggingFilter : ClientRequestFilter, ClientResponseFilter {
    companion object {
        private val LOG = ZacLogger.getLogger(JsonLoggingFilter::class)
        private val LOG_LEVEL = Level.FINE

        const val START_TIME_PROPERTY = "nl.info.zac.util.JsonLoggingFilter.startTime"

        // The request property in which a MicroProfile Rest Client puts the client method that it invokes
        const val INVOKED_METHOD_PROPERTY = "org.eclipse.microprofile.rest.client.invokedMethod"
    }

    override fun filter(requestContext: ClientRequestContext) {
        if (LOG.isLoggable(LOG_LEVEL)) {
            requestContext.setProperty(START_TIME_PROPERTY, System.nanoTime())
        }
    }

    override fun filter(requestContext: ClientRequestContext, responseContext: ClientResponseContext) {
        val startTime = requestContext.getProperty(START_TIME_PROPERTY) as? Long ?: return
        val status = responseContext.status
        LOG.log(
            LOG_LEVEL,
            OutboundRestCall(
                method = requestContext.method,
                host = requestContext.uri?.host,
                operation = (requestContext.getProperty(INVOKED_METHOD_PROPERTY) as? Method)?.toOperation(),
                status = status,
                outcome = if (status.isSuccessOrRedirection()) Outcome.SUCCESS else Outcome.FAILURE,
                durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)
            )
        )
    }

    private fun Method.toOperation() = "${declaringClass.simpleName}.$name"

    private fun Int.isSuccessOrRedirection() =
        Response.Status.Family.familyOf(this).let {
            it == Response.Status.Family.SUCCESSFUL || it == Response.Status.Family.REDIRECTION
        }
}
