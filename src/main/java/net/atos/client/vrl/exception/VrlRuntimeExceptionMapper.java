/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.vrl.exception;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

public class VrlRuntimeExceptionMapper implements ResponseExceptionMapper<RuntimeException> {

    @Override
    public boolean handles(final int status, final MultivaluedMap<String, Object> headers) {
        return status >= Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    }

    @Override
    public RuntimeException toThrowable(final Response response) {
        return new RuntimeException(
                String.format(
                        "Server response from VNG Referentielijsten: %d (%s)",
                        response.getStatus(),
                        response.getStatusInfo()
                )
        );
    }
}
