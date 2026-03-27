/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.or.shared.exception

import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.core.Response
import nl.info.client.or.shared.model.ORError
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper

/**
 * Maps all responses with status code greater than 400 (Bad Request) and less than 500 (Internal Server Error)
 * from the Object Registration APIs.
 * These responses are expected to have a JSON payload according to
 * [the Problem Details Standard](https://datatracker.ietf.org/doc/html/rfc7807).
 * 400 (Bad Request) status codes are handled by [ORValidationErrorExceptionMapper]
 */
class ORErrorExceptionMapper : ResponseExceptionMapper<ORErrorException> {
    override fun handles(status: Int, headers: MultivaluedMap<String, Any>) =
        Response.Status.BAD_REQUEST.statusCode < status && status < Response.Status.INTERNAL_SERVER_ERROR.statusCode

    override fun toThrowable(response: Response) =
        ORErrorException(response.readEntity(ORError::class.java))
}
