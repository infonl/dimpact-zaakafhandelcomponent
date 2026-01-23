/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.klant.util;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

import nl.info.zac.authentication.LoggedInUser;

public class KlantClientHeadersFactory implements ClientHeadersFactory {

    @Inject
    private Instance<LoggedInUser> loggedInUserInstance;

    @Inject
    @ConfigProperty(name = "KLANTINTERACTIES_API_TOKEN")
    private String token;

    @Override
    public MultivaluedMap<String, String> update(
            final MultivaluedMap<String, String> incomingHeaders,
            final MultivaluedMap<String, String> outgoingHeaders
    ) {
        outgoingHeaders.add(HttpHeaders.AUTHORIZATION, "Token " + token);
        return outgoingHeaders;
    }
}
