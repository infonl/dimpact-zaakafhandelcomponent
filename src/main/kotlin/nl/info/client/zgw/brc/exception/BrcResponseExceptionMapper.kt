/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.brc.exception

import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper

class BrcResponseExceptionMapper : ResponseExceptionMapper<RuntimeException> {
    override fun handles(status: Int, headers: MultivaluedMap<String, Any>): Boolean {
        return status >= Response.Status.INTERNAL_SERVER_ERROR.statusCode
    }

    override fun toThrowable(response: Response) =
        BrcRuntimeException(
            "Server response from the BRC API implementation: ${response.status} (${response.statusInfo})"
        )
}
