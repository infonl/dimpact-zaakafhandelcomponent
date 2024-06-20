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
import net.atos.client.zgw.brc.BrcClientService
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.zrc.ZRCClientService
import net.atos.client.zgw.ztc.ZTCClientService
import java.net.ConnectException
import java.net.UnknownHostException
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
        const val ERROR_CODE_BRC_CLIENT = "msg.error.brc.client.exception"
        const val ERROR_CODE_DRC_CLIENT = "msg.error.drc.client.exception"
        const val ERROR_CODE_ZRC_CLIENT = "msg.error.zrc.client.exception"
        const val ERROR_CODE_ZTC_CLIENT = "msg.error.ztc.client.exception"
        const val ERROR_CODE_GENERIC_SERVER = "msg.error.server.generic"
    }

    /**
     * Converts an exception to a JAX-RS response.
     */
    override fun toResponse(exception: Exception): Response =
        // Handle JAX-RS web application exceptions which are not server errors (5xx)
        // by passing on the exception response status and exception message.
        // These are typically 4xx family errors which we do not want to log.
        if (exception is WebApplicationException &&
            Response.Status.Family.familyOf(exception.response.status) != Response.Status.Family.SERVER_ERROR
        ) {
            Response.status(exception.response.status)
                .type(MediaType.APPLICATION_JSON)
                .entity(getJSONMessage(errorMessage = exception.message ?: ERROR_CODE_GENERIC_SERVER))
                .build()
        } else if (exception is ProcessingException && exception.cause?.let {
                it is ConnectException || it is UnknownHostException
            } == true
        ) {
            handleProcessingException(exception)
        } else {
            generateServerErrorResponse(exception = exception, exceptionMessage = exception.message)
        }

    /**
     * Handle JAX-RS processing exceptions which can be thrown by the various
     * ZAC REST clients, typically when the underlying REST client cannot connect
     * to the external service, by checking if the exception's stacktrace
     * contains the name of the ZAC REST client service class.
     * If so we generate a response with a specific error code, and we
     * log the exception.
     * Note that unfortunately we cannot map on the actual ZAC REST client class itself
     * since that class is proxied by the Eclipse Microprofile framework and is therefore not
     * shown in the stacktrace.
     */
    private fun handleProcessingException(exception: Exception): Response {
        val stackTrace = exception.stackTraceToString()
        return when {
            stackTrace.contains(BrcClientService::class.simpleName!!) -> {
                generateServerErrorResponse(exception = exception, errorCode = ERROR_CODE_BRC_CLIENT)
            }
            stackTrace.contains(DrcClientService::class.simpleName!!) -> {
                generateServerErrorResponse(exception = exception, errorCode = ERROR_CODE_DRC_CLIENT)
            }
            stackTrace.contains(ZRCClientService::class.simpleName!!) -> {
                generateServerErrorResponse(exception = exception, errorCode = ERROR_CODE_ZRC_CLIENT)
            }
            stackTrace.contains(ZTCClientService::class.simpleName!!) -> {
                generateServerErrorResponse(exception = exception, errorCode = ERROR_CODE_ZTC_CLIENT)
            }
            else -> {
                generateServerErrorResponse(exception = exception, exceptionMessage = exception.message)
            }
        }
    }

    private fun generateServerErrorResponse(
        exception: Exception,
        errorCode: String? = null,
        exceptionMessage: String? = null
    ): Response =
        Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .type(MediaType.APPLICATION_JSON)
            .entity(
                getJSONMessage(
                    errorMessage = errorCode ?: ERROR_CODE_GENERIC_SERVER,
                    exceptionMessage = exceptionMessage
                )
            )
            .build().also {
                LOG.log(
                    Level.SEVERE,
                    exceptionMessage ?: "Exception was thrown. Returning response with error code $errorCode.",
                    exception
                )
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
