/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.exception

import jakarta.ws.rs.ProcessingException
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import net.atos.client.bag.BagClientService
import net.atos.client.klant.KlantClientService
import net.atos.client.or.`object`.ObjectsClientService
import net.atos.client.zgw.drc.DrcClientService
import net.atos.client.zgw.drc.exception.DrcRuntimeException
import nl.info.client.brp.BrpClientService
import nl.info.client.zgw.brc.BrcClientService
import nl.info.client.zgw.brc.exception.BrcRuntimeException
import nl.info.client.zgw.shared.exception.ZgwRuntimeException
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.exception.ZrcRuntimeException
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.exception.ZtcRuntimeException
import nl.info.zac.app.exception.RestExceptionResponseBuilder.createResponse
import nl.info.zac.app.exception.RestExceptionResponseBuilder.generateResponse
import nl.info.zac.app.exception.RestExceptionResponseBuilder.generateServerErrorResponse
import nl.info.zac.exception.ErrorCode.ERROR_CODE_BAG_CLIENT
import nl.info.zac.exception.ErrorCode.ERROR_CODE_BETROKKENE_WAS_ALREADY_ADDED_TO_ZAAK
import nl.info.zac.exception.ErrorCode.ERROR_CODE_BRC_CLIENT
import nl.info.zac.exception.ErrorCode.ERROR_CODE_BRP_CLIENT
import nl.info.zac.exception.ErrorCode.ERROR_CODE_CASE_WITH_DECISION_CANNOT_BE_TERMINATION
import nl.info.zac.exception.ErrorCode.ERROR_CODE_DRC_CLIENT
import nl.info.zac.exception.ErrorCode.ERROR_CODE_FORBIDDEN
import nl.info.zac.exception.ErrorCode.ERROR_CODE_KLANTINTERACTIES_CLIENT
import nl.info.zac.exception.ErrorCode.ERROR_CODE_OBJECTS_CLIENT
import nl.info.zac.exception.ErrorCode.ERROR_CODE_SERVER_GENERIC
import nl.info.zac.exception.ErrorCode.ERROR_CODE_ZRC_CLIENT
import nl.info.zac.exception.ErrorCode.ERROR_CODE_ZTC_CLIENT
import nl.info.zac.exception.InputValidationFailedException
import nl.info.zac.exception.ServerErrorException
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.zaak.exception.BetrokkeneIsAlreadyAddedToZaakException
import nl.info.zac.zaak.exception.ZaakWithADecisionCannotBeTerminatedException
import java.net.ConnectException
import java.net.UnknownHostException
import java.util.concurrent.ExecutionException

/**
 * Maps exceptions to JAX-RS responses for use in REST responses.
 */
@Provider
class RestExceptionMapper : ExceptionMapper<Exception> {

    /**
     * Converts an exception to a JAX-RS response based on the exception type.
     * <p>
     * The output response format for most exceptions follows this structure:
     * <pre>
     * {"message": errorCode}
     * </pre>
     * Example:
     * <pre>
     * {"message": "msg.error.case.has.open.subcases"}
     * </pre>
     *
     * @param exception The exception to be converted into a response.
     * @return A JAX-RS Response object containing the appropriate error message.
     */
    @Suppress("CyclomaticComplexMethod", "LongMethod")
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
            exception is PolicyException -> generateResponse(
                responseStatus = Response.Status.FORBIDDEN,
                errorCode = ERROR_CODE_FORBIDDEN,
                exception = exception
            )
            exception is BetrokkeneIsAlreadyAddedToZaakException -> generateResponse(
                responseStatus = Response.Status.CONFLICT,
                errorCode = ERROR_CODE_BETROKKENE_WAS_ALREADY_ADDED_TO_ZAAK,
                exception = exception
            )
            exception is InputValidationFailedException -> generateResponse(
                responseStatus = Response.Status.BAD_REQUEST,
                errorCode = exception.errorCode ?: ERROR_CODE_SERVER_GENERIC,
                exception = exception
            )
            exception is ServerErrorException -> generateResponse(
                responseStatus = Response.Status.INTERNAL_SERVER_ERROR,
                errorCode = exception.errorCode,
                exception = exception
            )
            exception is ZaakWithADecisionCannotBeTerminatedException -> generateResponse(
                responseStatus = Response.Status.BAD_REQUEST,
                errorCode = ERROR_CODE_CASE_WITH_DECISION_CANNOT_BE_TERMINATION,
                exception = exception
            )
            // fall back to generic server error
            else -> generateServerErrorResponse(exception = exception, exceptionMessage = exception.message)
        }

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
            // fall back to generic server error
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
                it.contains(KlantClientService::class.simpleName!!) ->
                    generateServerErrorResponse(exception = exception, errorCode = ERROR_CODE_KLANTINTERACTIES_CLIENT)
                it.contains(ZrcClientService::class.simpleName!!) ->
                    generateServerErrorResponse(exception = exception, errorCode = ERROR_CODE_ZRC_CLIENT)
                it.contains(ZtcClientService::class.simpleName!!) ->
                    generateServerErrorResponse(exception = exception, errorCode = ERROR_CODE_ZTC_CLIENT)
                else -> generateServerErrorResponse(exception)
            }
        }
}
