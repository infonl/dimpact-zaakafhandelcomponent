/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.kvk.exception;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

public class KvkClientNoResultResponseExceptionMapper implements ResponseExceptionMapper<KvkClientNoResultException> {

    @Override
    public boolean handles(final int status, final MultivaluedMap<String, Object> headers) {
        return status == Response.Status.NOT_FOUND.getStatusCode();
    }

    @Override
    public KvkClientNoResultException toThrowable(final Response response) {
        return new KvkClientNoResultException("No results found for KVK search");
    }
}
