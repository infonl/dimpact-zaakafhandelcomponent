/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.brc.exception;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

public class BrcRuntimeExceptionMapper implements ResponseExceptionMapper<RuntimeException> {

    @Override
    public boolean handles(final int status, final MultivaluedMap<String, Object> headers) {
        return status >= Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    }

    @Override
    public RuntimeException toThrowable(final Response response) {
        return new BrcRuntimeException(
                String.format(
                        "Server response from the BRC API implementation: %d (%s)",
                        response.getStatus(),
                        response.getStatusInfo()
                )
        );
    }
}
