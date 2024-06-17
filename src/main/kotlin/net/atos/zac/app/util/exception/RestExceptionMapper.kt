/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.util.exception

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.ws.rs.ProcessingException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import net.atos.client.zgw.ztc.ZTCClientService
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Maps exceptions to JAX-RS responses for use in REST responses.
 */
@Provider
class RestExceptionMapper : ExceptionMapper<Exception> {
    companion object {
        private val LOG = Logger.getLogger(RestExceptionMapper::class.java.name)
        const val JSON_CONVERSION_ERROR_MESSAGE = "Failed to convert exception to JSON"
        const val ERROR_CODE_ZTC_CLIENT = "msg.error.ztc.client.exception"
        const val ERROR_CODE_GENERIC_SERVER = "msg.error.server.generic"
    }

    /**
     * Converts an exception to a JAX-RS response.
     */
    override fun toResponse(exception: Exception): Response =
        if (exception is WebApplicationException &&
            Response.Status.Family.familyOf(exception.response.status) != Response.Status.Family.SERVER_ERROR
        ) {
            Response.status(exception.response.status)
                .type(MediaType.APPLICATION_JSON)
                .entity(getJSONMessage(exception.message ?: ERROR_CODE_GENERIC_SERVER))
                .build()
        } else if (
            exception is ProcessingException &&
            exception.stackTraceToString().contains(ZTCClientService::class.simpleName!!)
        ) {
            LOG.log(Level.SEVERE, "Exception was thrown by ZTC client", exception)
            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(getJSONMessage(ERROR_CODE_ZTC_CLIENT))
                .build()
        } else {
            LOG.log(Level.SEVERE, exception.message, exception)
            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(getJSONMessage(ERROR_CODE_GENERIC_SERVER, exception.message))
                .build()
        }

    private fun getJSONMessage(errorMessage: String, exceptionMessage: String? = null) =
        try {
            val errorJsonHashMap = mutableMapOf(
                "message" to errorMessage
            )
            exceptionMessage?.let {
                errorJsonHashMap["exception"] = it
            }
            ObjectMapper().writeValueAsString(errorJsonHashMap)
        } catch (jsonProcessingException: JsonProcessingException) {
            LOG.log(Level.SEVERE, JSON_CONVERSION_ERROR_MESSAGE, jsonProcessingException)
            JSON_CONVERSION_ERROR_MESSAGE
        }
}
