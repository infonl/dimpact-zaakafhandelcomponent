/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.ztc.exception;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

public class ZtcRuntimeExceptionMapper implements ResponseExceptionMapper<RuntimeException> {

    @Override
    public boolean handles(final int status, final MultivaluedMap<String, Object> headers) {
        return status >= Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    }

    @Override
    public RuntimeException toThrowable(final Response response) {
        return new ZtcRuntimeException(
                String.format(
                        "Server response from the ZTC API implementation: %d (%s)",
                        response.getStatus(),
                        response.getStatusInfo()
                )
        );
    }
}
