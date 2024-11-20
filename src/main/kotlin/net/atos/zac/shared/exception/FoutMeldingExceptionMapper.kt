/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.shared.exception

import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider

/**
 * Exception mapper to catch [FoutmeldingException] thrown and convert them to a [Response] which is returned to
 * the frontend for further processing.
 */
@Provider
class FoutMeldingExceptionMapper : ExceptionMapper<FoutmeldingException?> {
    override fun toResponse(e: FoutmeldingException?): Response =
        Response.status(Response.Status.BAD_REQUEST).entity(e?.message).build()
}
