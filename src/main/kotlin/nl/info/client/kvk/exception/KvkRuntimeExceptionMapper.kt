/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.kvk.exception

import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper

class KvkRuntimeExceptionMapper : ResponseExceptionMapper<KvkClientException> {
    override fun handles(status: Int, headers: MultivaluedMap<String, Any>): Boolean =
        status >= Response.Status.INTERNAL_SERVER_ERROR.statusCode

    override fun toThrowable(response: Response) =
        KvkClientException("Server response from the KVK API: '${response.status}' (${response.statusInfo})")
}
