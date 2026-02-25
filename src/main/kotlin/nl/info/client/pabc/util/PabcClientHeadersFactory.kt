/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.pabc.util

import jakarta.ws.rs.core.MultivaluedMap
import org.eclipse.microprofile.config.ConfigProvider
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory

class PabcClientHeadersFactory : ClientHeadersFactory {
    companion object {
        private const val API_KEY_HEADER_FIELD = "X-API-KEY"

        private val API_KEY = ConfigProvider.getConfig().getValue(
            "pabc.api.key",
            String::class.java
        )
    }

    override fun update(
        incomingHeaders: MultivaluedMap<String, String>,
        clientOutgoingHeaders: MultivaluedMap<String, String>
    ): MultivaluedMap<String, String> {
        clientOutgoingHeaders.add(API_KEY_HEADER_FIELD, API_KEY)
        return clientOutgoingHeaders
    }
}
