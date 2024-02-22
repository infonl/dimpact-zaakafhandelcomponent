/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.shared.exception;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

import net.atos.client.zgw.shared.model.ValidatieFout;

/**
 *
 */
public class ValidatieFoutExceptionMapper
    implements ResponseExceptionMapper<ValidatieFoutException> {

  @Override
  public boolean handles(final int status, final MultivaluedMap<String, Object> headers) {
    return status == Response.Status.BAD_REQUEST.getStatusCode();
  }

  @Override
  public ValidatieFoutException toThrowable(final Response response) {
    return new ValidatieFoutException(response.readEntity(ValidatieFout.class));
  }
}
