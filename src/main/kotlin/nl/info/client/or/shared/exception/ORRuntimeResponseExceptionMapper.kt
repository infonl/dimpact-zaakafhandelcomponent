/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.or.shared.exception

import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper

class ORRuntimeResponseExceptionMapper : ResponseExceptionMapper<RuntimeException> {
    override fun handles(status: Int, headers: MultivaluedMap<String, Any>) =
        status >= Response.Status.INTERNAL_SERVER_ERROR.statusCode

    override fun toThrowable(response: Response) =
        ORRuntimeException(
            "Server response from object registratie: ${response.status} (${response.statusInfo})"
        )
}
