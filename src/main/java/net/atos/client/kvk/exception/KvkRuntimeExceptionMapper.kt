/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.kvk.exception

import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper

class KvkRuntimeExceptionMapper : ResponseExceptionMapper<RuntimeException?> {
    override fun handles(status: Int, headers: MultivaluedMap<String?, Any?>?): Boolean {
        return status >= Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()
    }

    override fun toThrowable(response: Response): RuntimeException {
        return RuntimeException(
            String.format(
                "Server response from KVK zoeken: %d (%s)", response.getStatus(), response
                    .getStatusInfo()
            )
        )
    }
}
