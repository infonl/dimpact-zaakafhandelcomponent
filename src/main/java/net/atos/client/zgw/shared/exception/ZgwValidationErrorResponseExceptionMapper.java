/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.shared.exception;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

import net.atos.client.zgw.shared.model.ValidationZgwError;

/**
 * Maps all responses with status code 400 (Bad Request) from the ZGW APIs to {@link ZgwValidationErrorException}s.
 * <p>
 * These responses are expected to have a JSON payload according to
 * <a href="https://datatracker.ietf.org/doc/html/rfc7807">the Problem Details Standard</a>.
 */
public class ZgwValidationErrorResponseExceptionMapper implements ResponseExceptionMapper<ZgwValidationErrorException> {

    @Override
    public boolean handles(final int status, final MultivaluedMap<String, Object> headers) {
        return status == Response.Status.BAD_REQUEST.getStatusCode();
    }

    @Override
    public ZgwValidationErrorException toThrowable(final Response response) {
        return new ZgwValidationErrorException(response.readEntity(ValidationZgwError.class));
    }
}
