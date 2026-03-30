/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.client.zgw.drc.exception

import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper

class DrcRuntimeResponseExceptionMapper : ResponseExceptionMapper<RuntimeException> {

    override fun handles(status: Int, headers: MultivaluedMap<String, Any>): Boolean =
        status >= Response.Status.INTERNAL_SERVER_ERROR.statusCode

    override fun toThrowable(response: Response): RuntimeException =
        DrcRuntimeException(
            "Server response from the DRC API implementation: ${response.status} (${response.statusInfo})"
        )
}
