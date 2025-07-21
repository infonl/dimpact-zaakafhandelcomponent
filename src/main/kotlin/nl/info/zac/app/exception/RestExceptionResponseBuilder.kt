/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.exception

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import nl.info.zac.exception.ErrorCode
import nl.info.zac.exception.ErrorCode.ERROR_CODE_SERVER_GENERIC
import nl.info.zac.log.log
import java.util.logging.Level
import java.util.logging.Logger

object RestExceptionResponseBuilder {

    private val LOG = Logger.getLogger(RestExceptionResponseBuilder::class.java.name)
    const val JSON_CONVERSION_ERROR_MESSAGE = "Failed to convert exception to JSON"

    fun createResponse(exception: WebApplicationException): Response =
        Response.status(exception.response.status)
            .type(MediaType.APPLICATION_JSON)
            .entity(getJSONMessage(errorMessage = exception.message ?: ERROR_CODE_SERVER_GENERIC.value))
            .build()

    fun generateResponse(
        responseStatus: Response.Status,
        errorCode: ErrorCode,
        exception: Exception,
        exceptionMessage: String? = null
    ): Response = Response.status(responseStatus)
        .type(MediaType.APPLICATION_JSON)
        .entity(
            getJSONMessage(
                errorMessage = errorCode.value,
                exceptionMessage = exceptionMessage
            )
        )
        .build().also {
            log(
                logger = LOG,
                level = if (responseStatus == Response.Status.INTERNAL_SERVER_ERROR) Level.SEVERE else Level.FINE,
                message = exception.message
                    ?: "Exception was thrown. Returning response with error message: '${errorCode.value}'.",
                throwable = exception
            )
        }

    fun generateServerErrorResponse(
        exception: Exception,
        errorCode: ErrorCode? = null,
        exceptionMessage: String? = null
    ) = generateResponse(
        responseStatus = Response.Status.INTERNAL_SERVER_ERROR,
        errorCode = errorCode ?: ERROR_CODE_SERVER_GENERIC,
        exception = exception,
        exceptionMessage = exceptionMessage
    )

    private fun getJSONMessage(errorMessage: String, exceptionMessage: String? = null) =
        try {
            val errorJsonHashMap = mutableMapOf("message" to errorMessage)
            exceptionMessage?.let { errorJsonHashMap["exception"] = it }
            ObjectMapper().writeValueAsString(errorJsonHashMap)
        } catch (jsonProcessingException: JsonProcessingException) {
            log(LOG, Level.SEVERE, JSON_CONVERSION_ERROR_MESSAGE, jsonProcessingException)
            JSON_CONVERSION_ERROR_MESSAGE
        }
}
