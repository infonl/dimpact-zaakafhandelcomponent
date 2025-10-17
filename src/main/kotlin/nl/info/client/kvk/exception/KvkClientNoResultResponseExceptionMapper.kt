/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.kvk.exception

import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper

class KvkClientNoResultResponseExceptionMapper : ResponseExceptionMapper<KvkClientNoResultException> {
    override fun handles(status: Int, headers: MultivaluedMap<String, Any>): Boolean {
        return status == Response.Status.NOT_FOUND.statusCode
    }

    override fun toThrowable(response: Response): KvkClientNoResultException {
        return KvkClientNoResultException("No results found for KVK search")
    }
}
