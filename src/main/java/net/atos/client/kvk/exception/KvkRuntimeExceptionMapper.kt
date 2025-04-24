/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.kvk.exception

import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper

class KvkRuntimeExceptionMapper : ResponseExceptionMapper<RuntimeException> {
    override fun handles(status: Int, headers: MultivaluedMap<String, Any>): Boolean =
        status >= Response.Status.INTERNAL_SERVER_ERROR.statusCode

    override fun toThrowable(response: Response) =
        RuntimeException("Server response from KVK zoeken: '${response.status}' (${response.statusInfo})")
}
