/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.shared.exception;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

import net.atos.client.zgw.shared.model.Fout;

/**
 *
 */
public class FoutExceptionMapper implements ResponseExceptionMapper<FoutException> {

  @Override
  public boolean handles(final int status, final MultivaluedMap<String, Object> headers) {
    return Response.Status.BAD_REQUEST.getStatusCode() <= status
        && status < Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
  }

  @Override
  public FoutException toThrowable(final Response response) {
    return new FoutException(response.readEntity(Fout.class));
  }
}
