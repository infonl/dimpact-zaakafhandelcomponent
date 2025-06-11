/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.brp.exception

import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper

/**
 * Maps HTTP error responses from the BRP API to a [BrpRuntimeException].
 */
class BrpResponseExceptionMapper : ResponseExceptionMapper<RuntimeException> {
    override fun toThrowable(response: Response) =
        BrpRuntimeException(
            "Received error response from the BRP API implementation: ${response.status} (${ response.statusInfo})"
        )
}
