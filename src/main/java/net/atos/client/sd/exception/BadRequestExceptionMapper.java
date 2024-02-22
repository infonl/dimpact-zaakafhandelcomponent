/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.sd.exception;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

public class BadRequestExceptionMapper implements ResponseExceptionMapper<BadRequestException> {

  @Override
  public boolean handles(final int status, final MultivaluedMap<String, Object> headers) {
    return status == Response.Status.BAD_REQUEST.getStatusCode();
  }

  @Override
  public BadRequestException toThrowable(final Response response) {
    return new BadRequestException();
  }
}
