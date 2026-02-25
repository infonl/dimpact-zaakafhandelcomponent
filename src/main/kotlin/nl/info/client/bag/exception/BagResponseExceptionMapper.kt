/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.bag.exception

import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper

class BagResponseExceptionMapper : ResponseExceptionMapper<RuntimeException> {

    override fun handles(status: Int, headers: MultivaluedMap<String, Any>): Boolean =
        status >= Response.Status.INTERNAL_SERVER_ERROR.statusCode

    override fun toThrowable(response: Response): RuntimeException =
        RuntimeException("Server response from BAG: ${response.status} (${response.statusInfo})")
}
