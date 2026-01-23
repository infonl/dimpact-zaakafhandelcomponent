/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.ztc.exception

import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper

class ZtcResponseExceptionMapper : ResponseExceptionMapper<RuntimeException> {
    override fun handles(status: Int, headers: MultivaluedMap<String, Any>): Boolean {
        return status >= Response.Status.INTERNAL_SERVER_ERROR.statusCode
    }

    override fun toThrowable(response: Response) = ZtcRuntimeException(
        "Server response from the ZTC API implementation: ${response.status} (${ response.statusInfo})",
    )
}
