/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.shared.exception

import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import net.atos.client.zgw.shared.exception.ValidationErrorException
import nl.info.zac.app.exception.RestExceptionResponseBuilder.generateResponse
import nl.info.zac.exception.ErrorCode

/**
 * Exception mapper to catch [ValidationErrorException] thrown and convert them to a [Response] which is returned to
 * the frontend for further processing.
 */
@Provider
class ValidatieFoutExceptionMapper : ExceptionMapper<ValidationErrorException> {
    override fun toResponse(validationErrorException: ValidationErrorException): Response {
        return generateResponse(
            responseStatus = Response.Status.BAD_REQUEST,
            errorCode = ErrorCode.ERROR_CODE_VALIDATION_GENERIC,
            exception = validationErrorException,
            exceptionMessage = validationErrorException.validatieFout.invalidParams.toString()
        )
    }
}
