/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.brp.util

import jakarta.ws.rs.core.MultivaluedMap
import org.eclipse.microprofile.config.ConfigProvider
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory

class BRPClientHeadersFactory : ClientHeadersFactory {
    companion object {
        private const val X_API_KEY = "X-API-KEY"
        private val API_KEY = ConfigProvider.getConfig().getValue("brp.api.key", String::class.java)
    }

    override fun update(
        incomingHeaders: MultivaluedMap<String, String>,
        clientOutgoingHeaders: MultivaluedMap<String, String>
    ): MultivaluedMap<String, String> {
        clientOutgoingHeaders.add(X_API_KEY, API_KEY)
        return clientOutgoingHeaders
    }
}
