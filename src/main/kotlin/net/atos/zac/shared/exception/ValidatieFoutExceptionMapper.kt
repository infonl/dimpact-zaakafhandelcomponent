/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.shared.exception

import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import net.atos.client.zgw.shared.exception.ValidatieFoutException

/**
 * Exceptionmapper om [ValidatieFoutException] die gegooit zijn op te vangen en om te zetten
 * naar een [Response] welke teruggeven wordt naar de frontend voor verdere afwerking.
 */
@Provider
class ValidatieFoutExceptionMapper : ExceptionMapper<ValidatieFoutException?> {
    override fun toResponse(validatieFoutException: ValidatieFoutException?): Response =
        validatieFoutException?.validatieFout.toString().let {
            Response.status(Response.Status.BAD_REQUEST).entity(it).build()
        }
}
