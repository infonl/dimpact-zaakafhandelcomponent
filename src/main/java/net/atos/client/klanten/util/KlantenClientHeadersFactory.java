/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.klanten.util;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import net.atos.client.util.JWTTokenGenerator;
import net.atos.zac.authentication.LoggedInUser;

public class KlantenClientHeadersFactory implements ClientHeadersFactory {

  @Inject private Instance<LoggedInUser> loggedInUserInstance;

  @Inject
  @ConfigProperty(name = "KLANTEN_API_CLIENTID")
  private String clientId;

  @Inject
  @ConfigProperty(name = "KLANTEN_API_SECRET")
  private String secret;

  @Override
  public MultivaluedMap<String, String> update(
      final MultivaluedMap<String, String> incomingHeaders,
      final MultivaluedMap<String, String> outgoingHeaders) {
    outgoingHeaders.add(
        HttpHeaders.AUTHORIZATION,
        JWTTokenGenerator.generate(clientId, secret, loggedInUserInstance.get()));
    return outgoingHeaders;
  }
}
