/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.util

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import org.apache.commons.lang3.exception.ExceptionUtils
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Implementatie van ExceptionMapper. Alle exceptions worden gecatched door de JAX-RS runtime en gemapped naar een [Response].
 */
@Provider
class RESTExceptionMapper : ExceptionMapper<Exception> {
    companion object {
        private val LOG = Logger.getLogger(RESTExceptionMapper::class.java.name)
        const val JSON_CONVERSION_ERROR_MESSAGE = "Failed to convert exception to JSON"
    }

    /**
     * Retourneert een [Response] naar de client.
     */
    override fun toResponse(exception: Exception): Response {
        if (exception is WebApplicationException &&
            Response.Status.Family.familyOf(exception.response.status) != Response.Status.Family.SERVER_ERROR
        ) {
            return Response.status(exception.response.status)
                .type(MediaType.APPLICATION_JSON)
                .entity(getJSONMessage(exception, exception.message))
                .build()
        } else {
            LOG.log(Level.SEVERE, exception.message, exception)
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(getJSONMessage(exception, "Algemene Fout"))
                .build()
        }
    }

    private fun getJSONMessage(exception: Exception, melding: String?): String {
        val data: MutableMap<String, Any?> = HashMap()
        melding?.let { data["message"] = it }
        data["exception"] = exception.message
        data["stackTrace"] = ExceptionUtils.getStackTrace(exception)
        return try {
            ObjectMapper().writeValueAsString(data)
        } catch (ioException: IOException) {
            LOG.log(Level.SEVERE, JSON_CONVERSION_ERROR_MESSAGE, ioException)
            JSON_CONVERSION_ERROR_MESSAGE
        }
    }
}
