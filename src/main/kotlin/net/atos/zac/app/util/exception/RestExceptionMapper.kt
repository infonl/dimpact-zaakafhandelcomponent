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
import net.atos.client.bag.BagClientService
import net.atos.client.brp.BrpClientService
import net.atos.client.klant.KlantClientService
import net.atos.client.or.`object`.ObjectsClientService
import net.atos.client.or.objecttype.ObjecttypesClientService
import net.atos.client.zgw.brc.BrcClientService
import net.atos.client.zgw.brc.exception.BrcRuntimeException
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.drc.exception.DrcRuntimeException
import net.atos.client.zgw.shared.exception.ZgwRuntimeException
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.exception.ZrcRuntimeException
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.client.zgw.ztc.exception.ZtcRuntimeException
import net.atos.zac.policy.exception.PolicyException
import java.net.ConnectException
import java.net.UnknownHostException
import java.util.concurrent.ExecutionException
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
        const val ERROR_CODE_BAG_CLIENT = "msg.error.bag.client.exception"
        const val ERROR_CODE_BRC_CLIENT = "msg.error.brc.client.exception"
        const val ERROR_CODE_BRP_CLIENT = "msg.error.brp.client.exception"
        const val ERROR_CODE_DRC_CLIENT = "msg.error.drc.client.exception"
        const val ERROR_CODE_KLANTINTERACTIES_CLIENT = "msg.error.klanten.client.exception"
        const val ERROR_CODE_OBJECTS_CLIENT = "msg.error.objects.client.exception"
        const val ERROR_CODE_OBJECTTYPES_CLIENT = "msg.error.objecttypes.client.exception"
        const val ERROR_CODE_ZRC_CLIENT = "msg.error.zrc.client.exception"
        const val ERROR_CODE_ZTC_CLIENT = "msg.error.ztc.client.exception"
        const val ERROR_CODE_GENERIC_SERVER = "msg.error.server.generic"
        const val ERROR_CODE_FORBIDDEN = "msg.error.server.forbidden"
        const val ERROR_CODE_SYSTEM_REFERENCE_TABLE_CANNOT_BE_DELETED = "msg.error.system.reference.table.cannot.be.deleted"
        const val ERROR_CODE_REFERENCE_TABLE_SYSTEM_VALUES_CANNOT_BE_CHANGED =
            "msg.error.system.reference.table.system.values.cannot.be.changed"
        const val ERROR_CODE_REFERENCE_TABLE_WITH_SAME_CODE_ALREADY_EXISTS = "msg.error.reference.table.with.same.code.already.exists"
        const val ERROR_CODE_REFERENCE_TABLE_IS_IN_USE_BY_ZAAKAFHANDELPARAMETERS =
            "msg.error.reference.table.is.in.use.by.zaakafhandelparameters"
    }

    /**
     * Converts an exception to a JAX-RS response depending on the exception type.
     */
    override fun toResponse(exception: Exception): Response =
        when {
            exception is WebApplicationException &&
                Response.Status.Family.familyOf(exception.response.status) != Response.Status.Family.SERVER_ERROR -> {
                createResponse(exception)
            }
            // handle execution exceptions thrown from asynchronous Java concurrent methods
            exception is ExecutionException && (exception.cause is WebApplicationException) &&
                Response.Status.Family.familyOf(
                    (
                        exception.cause as WebApplicationException
                        ).response.status
                ) != Response.Status.Family.SERVER_ERROR -> {
                createResponse(exception.cause as WebApplicationException)
            }
            exception is ZgwRuntimeException -> handleZgwRuntimeException(exception)
            exception is ProcessingException && (exception.cause is ConnectException || exception.cause is UnknownHostException) -> {
                handleProcessingException(exception)
            }
            exception is PolicyException -> generateForbiddenResponse(exception = exception)
            else -> generateServerErrorResponse(exception = exception, exceptionMessage = exception.message)
        }

    private fun createResponse(exception: WebApplicationException) =
        Response.status(exception.response.status)
            .type(MediaType.APPLICATION_JSON)
            .entity(getJSONMessage(errorMessage = exception.message ?: ERROR_CODE_GENERIC_SERVER))
            .build()

    private fun handleZgwRuntimeException(exception: ZgwRuntimeException): Response =
        when (exception) {
            is BrcRuntimeException -> generateServerErrorResponse(
                exception = exception,
                errorCode = ERROR_CODE_BRC_CLIENT
            )
            is DrcRuntimeException -> generateServerErrorResponse(
                exception = exception,
                errorCode = ERROR_CODE_DRC_CLIENT
            )
            is ZrcRuntimeException -> generateServerErrorResponse(
                exception = exception,
                errorCode = ERROR_CODE_ZRC_CLIENT
            )
            is ZtcRuntimeException -> generateServerErrorResponse(
                exception = exception,
                errorCode = ERROR_CODE_ZTC_CLIENT
            )
            else -> generateServerErrorResponse(exception = exception, exceptionMessage = exception.message)
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
    private fun handleProcessingException(exception: Exception): Response =
        exception.stackTraceToString().let {
            when {
                it.contains(BagClientService::class.simpleName!!) ->
                    generateServerErrorResponse(exception = exception, errorCode = ERROR_CODE_BAG_CLIENT)
                it.contains(BrcClientService::class.simpleName!!) ->
                    generateServerErrorResponse(exception = exception, errorCode = ERROR_CODE_BRC_CLIENT)
                it.contains(BrpClientService::class.simpleName!!) ->
                    generateServerErrorResponse(exception = exception, errorCode = ERROR_CODE_BRP_CLIENT)
                it.contains(DrcClientService::class.simpleName!!) ->
                    generateServerErrorResponse(exception = exception, errorCode = ERROR_CODE_DRC_CLIENT)
                it.contains(ObjectsClientService::class.simpleName!!) ->
                    generateServerErrorResponse(exception = exception, errorCode = ERROR_CODE_OBJECTS_CLIENT)
                it.contains(ObjecttypesClientService::class.simpleName!!) ->
                    generateServerErrorResponse(exception = exception, errorCode = ERROR_CODE_OBJECTTYPES_CLIENT)
                it.contains(KlantClientService::class.simpleName!!) ->
                    generateServerErrorResponse(exception = exception, errorCode = ERROR_CODE_KLANTINTERACTIES_CLIENT)
                it.contains(ZrcClientService::class.simpleName!!) ->
                    generateServerErrorResponse(exception = exception, errorCode = ERROR_CODE_ZRC_CLIENT)
                it.contains(ZtcClientService::class.simpleName!!) ->
                    generateServerErrorResponse(exception = exception, errorCode = ERROR_CODE_ZTC_CLIENT)
                else -> generateServerErrorResponse(exception = exception, exceptionMessage = exception.message)
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

    private fun generateForbiddenResponse(
        exception: Exception,
        exceptionMessage: String? = null
    ): Response =
        Response.status(Response.Status.FORBIDDEN)
            .type(MediaType.APPLICATION_JSON)
            .entity(
                getJSONMessage(
                    errorMessage = ERROR_CODE_FORBIDDEN,
                    exceptionMessage = exceptionMessage
                )
            )
            .build().also {
                LOG.log(
                    Level.FINE,
                    exceptionMessage ?: "Exception was thrown. Returning response with error code $ERROR_CODE_FORBIDDEN.",
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
