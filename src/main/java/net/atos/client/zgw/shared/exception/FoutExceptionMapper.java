/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.shared.exception;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

import net.atos.client.or.shared.exception.ValidatieFoutExceptionMapper;
import net.atos.client.zgw.shared.model.Fout;

/**
 * Maps all responses with as status code greater than 400 (Bad Request) and less than 500 (Internal Server Error)
 * from the Object Registration APIs.
 * These responses are expected to have a JSON payload according to
 * <a href="https://datatracker.ietf.org/doc/html/rfc7807">the Problem Details Standard</a>.
 * 400 (Bad Request) status codes are handled by {@link ValidatieFoutExceptionMapper}
 */
public class FoutExceptionMapper implements ResponseExceptionMapper<FoutException> {

    @Override
    public boolean handles(final int status, final MultivaluedMap<String, Object> headers) {
        return Response.Status.BAD_REQUEST.getStatusCode() < status && status < Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    }

    @Override
    public FoutException toThrowable(final Response response) {
        return new FoutException(response.readEntity(Fout.class));
    }
}
