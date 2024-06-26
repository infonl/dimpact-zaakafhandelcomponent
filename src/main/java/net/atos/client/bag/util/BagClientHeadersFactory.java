/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.bag.util;

import jakarta.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;


public class BagClientHeadersFactory implements ClientHeadersFactory {

    public static final String X_API_KEY = "X-Api-Key";

    public static final String API_KEY = ConfigProvider.getConfig().getValue("bag.api.key", String.class);

    @Override
    public MultivaluedMap<String, String> update(
            final MultivaluedMap<String, String> incomingHeaders,
            final MultivaluedMap<String, String> clientOutgoingHeaders
    ) {
        clientOutgoingHeaders.add(X_API_KEY, API_KEY);
        return clientOutgoingHeaders;
    }
}
