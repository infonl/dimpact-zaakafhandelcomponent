/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.vrl.exception;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;

public class CommunicatiekanaalNotFoundExceptionMapping
    implements ResponseExceptionMapper<CommunicatiekanaalNotFoundException> {

  @Override
  public boolean handles(final int status, final MultivaluedMap<String, Object> headers) {
    return status == Response.Status.NOT_FOUND.getStatusCode();
  }

  @Override
  public CommunicatiekanaalNotFoundException toThrowable(final Response response) {
    return new CommunicatiekanaalNotFoundException();
  }
}
