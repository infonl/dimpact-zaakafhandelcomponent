/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.util;

import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

import org.apache.commons.lang3.StringUtils;

/**
 * Factory for creating a JAX-RS {@link Client} that can be reused within the application.
 * Note that only one instance of such a client is created here.
 * And since it can be reused it is never closed.
 */
public final class JAXRSClientFactory {

    private static Client client;

    private JAXRSClientFactory() {
    }

    public static Client getOrCreateClient() {
        if (client == null) {
            client = createClient();
        }
        return client;
    }

    private static Client createClient() {
        final String proxyHost = System.getProperty("http.proxyHost");
        final String proxyPort = System.getProperty("http.proxyPort");
        try {
            final ClientBuilder clientBuilder = ClientBuilder.newBuilder().sslContext(SSLContext.getDefault());
            if (StringUtils.isNotEmpty(proxyHost) && StringUtils.isNumeric(proxyPort)) {
                clientBuilder
                        .property("org.jboss.resteasy.jaxrs.client.proxy.host", proxyHost)
                        .property("org.jboss.resteasy.jaxrs.client.proxy.port", proxyPort);
            }
            return clientBuilder.build();
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Failed to initialize JAX-RS client", e);
        }
    }
}
