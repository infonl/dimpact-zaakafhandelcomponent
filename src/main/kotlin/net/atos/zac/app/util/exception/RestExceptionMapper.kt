/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.util.exception

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import net.atos.zac.app.util.exception.RestExceptionMapper.Companion.JSON_CONVERSION_ERROR_MESSAGE
import net.atos.zac.app.util.exception.RestExceptionMapper.Companion.LOG
import org.apache.commons.lang3.exception.ExceptionUtils
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Implementatie van ExceptionMapper. Alle exceptions worden gecatched door de JAX-RS runtime en gemapped naar een [Response].
 */
@Provider
class RestExceptionMapper : ExceptionMapper<Exception> {
    companion object {
        private val LOG = Logger.getLogger(RestExceptionMapper::class.java.name)
        const val JSON_CONVERSION_ERROR_MESSAGE = "Failed to convert exception to JSON"
    }

    /**
     * Retourneert een [Response] naar de client.
     */
    override fun toResponse(exception: Exception): Response =
        if (exception is WebApplicationException &&
            Response.Status.Family.familyOf(exception.response.status) != Response.Status.Family.SERVER_ERROR
        ) {
            Response.status(exception.response.status)
                .type(MediaType.APPLICATION_JSON)
                .entity(getJSONMessage(exception, exception.message))
                .build()
        } else {
            LOG.log(Level.SEVERE, exception.message, exception)
            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(getJSONMessage(exception, "Algemene Fout"))
                .build()
        }

    private fun getJSONMessage(exception: Exception, melding: String?) =
        try {
            val data = HashMap<String, Any?>().apply {
                melding?.let { this["message"] = it }
                this["exception"] = exception.message
                this["stackTrace"] = ExceptionUtils.getStackTrace(exception)
            }
            ObjectMapper().writeValueAsString(data)
        } catch (ioException: IOException) {
            LOG.log(Level.SEVERE, JSON_CONVERSION_ERROR_MESSAGE, ioException)
            JSON_CONVERSION_ERROR_MESSAGE
        }
}
