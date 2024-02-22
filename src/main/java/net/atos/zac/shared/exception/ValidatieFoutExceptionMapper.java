/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.shared.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import net.atos.client.zgw.shared.exception.ValidatieFoutException;

/**
 * Exceptionmapper om {@link ValidatieFoutException} die gegooit zijn op te vangen en om te zetten
 * naar een {@link Response} welke teruggeven wordt naar de frontend voor verdere afwerking.
 */
@Provider
public class ValidatieFoutExceptionMapper implements ExceptionMapper<ValidatieFoutException> {

  @Override
  public Response toResponse(final ValidatieFoutException validatieFoutException) {
    final String validatieFouten = validatieFoutException.getValidatieFout().toString();
    return Response.status(Response.Status.BAD_REQUEST).entity(validatieFouten).build();
  }
}
