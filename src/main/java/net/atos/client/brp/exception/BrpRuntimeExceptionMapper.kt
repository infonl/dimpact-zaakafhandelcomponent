/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.brp.exception

import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper


class BrpRuntimeExceptionMapper : ResponseExceptionMapper<RuntimeException> {
    override fun handles(status: Int, headers: MultivaluedMap<String, Any>): Boolean {
        return status >= Response.Status.INTERNAL_SERVER_ERROR.statusCode
    }

    override fun toThrowable(response: Response): RuntimeException {
        return RuntimeException(
            String.format(
                "Server response from BRP: %d (%s)",
                response.status,
                response.statusInfo
            )
        )
    }
}
