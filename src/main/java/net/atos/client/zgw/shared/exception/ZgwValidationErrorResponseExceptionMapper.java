/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.shared.exception;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

import net.atos.client.zgw.shared.model.ValidatieZgwError;

/**
 * Maps all responses with status code 400 (Bad Request) from the ZGW APIs to {@link ValidationErrorException}s.
 * <p>
 * These responses are expected to have a JSON payload according to
 * <a href="https://datatracker.ietf.org/doc/html/rfc7807">the Problem Details Standard</a>.
 */
public class ZgwValidationErrorResponseExceptionMapper implements ResponseExceptionMapper<ValidationErrorException> {

    @Override
    public boolean handles(final int status, final MultivaluedMap<String, Object> headers) {
        return status == Response.Status.BAD_REQUEST.getStatusCode();
    }

    @Override
    public ValidationErrorException toThrowable(final Response response) {
        return new ValidationErrorException(response.readEntity(ValidatieZgwError.class));
    }
}
