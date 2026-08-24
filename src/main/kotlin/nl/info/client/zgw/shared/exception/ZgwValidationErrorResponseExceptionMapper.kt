/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared.exception

import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper
import nl.info.client.zgw.shared.model.ZgwValidationError

/**
 * Maps all responses with status code 400 (Bad Request) from the ZGW APIs to [ZgwValidationErrorException]s.
 *
 * These responses are expected to have a JSON payload according to
 * [the Problem Details Standard](https://datatracker.ietf.org/doc/html/rfc7807).
 */
class ZgwValidationErrorResponseExceptionMapper : ResponseExceptionMapper<ZgwValidationErrorException> {

    override fun handles(status: Int, headers: MultivaluedMap<String, Any>): Boolean =
        status == Response.Status.BAD_REQUEST.statusCode

    override fun toThrowable(response: Response): ZgwValidationErrorException =
        ZgwValidationErrorException(response.readEntity(ZgwValidationError::class.java))
}
