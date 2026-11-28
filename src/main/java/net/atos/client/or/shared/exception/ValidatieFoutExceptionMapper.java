/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.or.shared.exception;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

import net.atos.client.or.shared.model.ValidatieFout;

/**
 * Maps all responses with status code 400 (Bad Request) from the Object Registration APIs.
 * These responses are expected to have a JSON payload according to
 * <a href="https://datatracker.ietf.org/doc/html/rfc7807">the Problem Details Standard</a>.
 */
public class ValidatieFoutExceptionMapper implements ResponseExceptionMapper<ValidatieFoutException> {

    @Override
    public boolean handles(final int status, final MultivaluedMap<String, Object> headers) {
        return status == Response.Status.BAD_REQUEST.getStatusCode();
    }

    @Override
    public ValidatieFoutException toThrowable(final Response response) {
        return new ValidatieFoutException(response.readEntity(ValidatieFout.class));
    }
}
