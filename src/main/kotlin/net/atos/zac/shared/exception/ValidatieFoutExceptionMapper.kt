/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.shared.exception

import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import net.atos.client.zgw.shared.exception.ValidationErrorException

/**
 * Exception mapper to catch [ValidationErrorException] thrown and convert them to a [Response] which is returned to
 * the frontend for further processing.
 */
@Provider
class ValidatieFoutExceptionMapper : ExceptionMapper<ValidationErrorException> {
    override fun toResponse(validationErrorException: ValidationErrorException): Response =
        validationErrorException.validatieFout.toString().let {
            Response.status(Response.Status.BAD_REQUEST).entity(it).build()
        }
}
