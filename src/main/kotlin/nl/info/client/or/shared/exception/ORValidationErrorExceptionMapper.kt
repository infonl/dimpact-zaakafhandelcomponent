/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.or.shared.exception

import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.core.Response
import nl.info.client.or.shared.model.ORValidationError
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper

/**
 * Maps all responses with status code 400 (Bad Request) from the Object Registration APIs.
 * These responses are expected to have a JSON payload according to
 * [the Problem Details Standard](https://datatracker.ietf.org/doc/html/rfc7807).
 */
class ORValidationErrorExceptionMapper : ResponseExceptionMapper<ORValidationErrorException> {
    override fun handles(status: Int, headers: MultivaluedMap<String, Any>) =
        status == Response.Status.BAD_REQUEST.statusCode

    override fun toThrowable(response: Response) =
        ORValidationErrorException(response.readEntity(ORValidationError::class.java))
}
