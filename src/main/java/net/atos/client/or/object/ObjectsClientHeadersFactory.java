/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.or.object;

import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;

import jakarta.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

/**
 *
 */
public class ObjectsClientHeadersFactory implements ClientHeadersFactory {

  private static final String TOKEN =
      ConfigProvider.getConfig().getValue("objects.api.token", String.class);

  @Override
  public MultivaluedMap<String, String> update(
      final MultivaluedMap<String, String> incomingHeaders,
      final MultivaluedMap<String, String> clientOutgoingHeaders) {
    clientOutgoingHeaders.add(AUTHORIZATION, String.format("Token %s", TOKEN));
    return clientOutgoingHeaders;
  }
}
