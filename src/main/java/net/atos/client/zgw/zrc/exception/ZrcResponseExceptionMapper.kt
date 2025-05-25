/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.zrc.exception

import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper

/**
 * Maps ZRC API responses with status code greater than or equal to 500 (Internal Server Error)
 * to a [ZrcRuntimeException]s.
 */
class ZrcResponseExceptionMapper : ResponseExceptionMapper<RuntimeException> {
    override fun handles(status: Int, headers: MultivaluedMap<String, Any>): Boolean {
        return status >= Response.Status.INTERNAL_SERVER_ERROR.statusCode
    }

    override fun toThrowable(response: Response) =
        ZrcRuntimeException(
            "Server response from the ZRC API implementation: ${response.status} (${response.statusInfo})",

        )
}
