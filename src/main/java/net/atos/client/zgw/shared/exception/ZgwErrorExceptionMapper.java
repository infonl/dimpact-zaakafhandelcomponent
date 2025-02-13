/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.shared.exception;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

import net.atos.client.or.shared.exception.ValidatieFoutExceptionMapper;
import net.atos.client.zgw.shared.model.ZgwError;

/**
 * Maps all responses with status code greater than 400 (Bad Request) and less than 500 (Internal Server Error)
 * except 404's (not found) from the ZGW API clients.
 * <p>
 * 404 status responses are not mapped here because we assume that these are properly handled by the client services
 * themselves and do not require handling here. If we do we would get duplicate handling of 404's.
 * <p>
 * These responses are expected to have a JSON payload according to
 * <a href="https://datatracker.ietf.org/doc/html/rfc7807">the Problem Details Standard</a>.
 * 400 (Bad Request) status codes are handled by {@link ValidatieFoutExceptionMapper}
 */
public class ZgwErrorExceptionMapper implements ResponseExceptionMapper<ZgwErrorException> {

    @Override
    public boolean handles(final int status, final MultivaluedMap<String, Object> headers) {
        return Response.Status.BAD_REQUEST.getStatusCode() < status &&
               status != Response.Status.NOT_FOUND.getStatusCode() &&
               status < Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    }

    @Override
    public ZgwErrorException toThrowable(final Response response) {
        return new ZgwErrorException(response.readEntity(ZgwError.class));
    }
}
