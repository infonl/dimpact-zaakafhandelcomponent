/*
 * SPDX-FileCopyrightText: 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.kvk.util;

import jakarta.ws.rs.core.MultivaluedMap;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;


public class KvkClientHeadersFactory implements ClientHeadersFactory {

    public static final String KVK_API_KEY_HEADER_FIELD = "apikey";

    public static final String API_KEY = ConfigProvider.getConfig().getValue("kvk.api.key",
            String.class);

    @Override
    public final MultivaluedMap<String, String> update(
            final MultivaluedMap<String, String> incomingHeaders,
            final MultivaluedMap<String, String> clientOutgoingHeaders
    ) {
        clientOutgoingHeaders.add(KVK_API_KEY_HEADER_FIELD, API_KEY);
        return clientOutgoingHeaders;
    }
}
