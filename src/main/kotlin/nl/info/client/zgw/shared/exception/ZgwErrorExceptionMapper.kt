/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared.exception

import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper
import nl.info.client.zgw.shared.model.ZgwError

/**
 * Maps all responses with status code greater than 400 (Bad Request) and less than 500 (Internal Server Error)
 * except 404's (not found) from the ZGW API clients.
 *
 * 404 status responses are not mapped here because we assume that these are properly handled by the client services
 * themselves and do not require handling here. If we do we would get duplicate handling of 404's.
 *
 * These responses are expected to have a JSON payload according to
 * [the Problem Details Standard](https://datatracker.ietf.org/doc/html/rfc7807).
 * 400 (Bad Request) status codes are handled by [ZgwValidationErrorResponseExceptionMapper]
 */
class ZgwErrorExceptionMapper : ResponseExceptionMapper<ZgwErrorException> {

    override fun handles(status: Int, headers: MultivaluedMap<String, Any>): Boolean =
        Response.Status.BAD_REQUEST.statusCode < status &&
            status != Response.Status.NOT_FOUND.statusCode &&
            status < Response.Status.INTERNAL_SERVER_ERROR.statusCode

    override fun toThrowable(response: Response): ZgwErrorException =
        ZgwErrorException(response.readEntity(ZgwError::class.java))
}
