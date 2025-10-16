/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.klant.exception

import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper

class KlantRuntimeResponseExceptionMapper : ResponseExceptionMapper<RuntimeException> {
    override fun handles(status: Int, headers: MultivaluedMap<String, Any>): Boolean {
        return status >= Response.Status.INTERNAL_SERVER_ERROR.statusCode
    }

    override fun toThrowable(response: Response): RuntimeException =
        KlantRuntimeException("Server response from Klanten: ${response.status} (${response.statusInfo})")
}
