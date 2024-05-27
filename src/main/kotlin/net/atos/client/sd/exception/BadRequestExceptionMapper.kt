/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.sd.exception

import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper

class BadRequestExceptionMapper : ResponseExceptionMapper<BadRequestException> {
    override fun handles(status: Int, headers: MultivaluedMap<String, Any>): Boolean =
        status == Response.Status.BAD_REQUEST.statusCode

    override fun toThrowable(response: Response): BadRequestException =
        BadRequestException()
}
