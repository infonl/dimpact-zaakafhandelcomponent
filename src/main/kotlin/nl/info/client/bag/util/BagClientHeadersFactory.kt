/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.bag.util

import jakarta.ws.rs.core.MultivaluedMap
import org.eclipse.microprofile.config.ConfigProvider
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory

class BagClientHeadersFactory : ClientHeadersFactory {

    companion object {
        const val X_API_KEY = "X-Api-Key"
        val API_KEY: String = ConfigProvider.getConfig().getValue("bag.api.key", String::class.java)
    }

    override fun update(
        incomingHeaders: MultivaluedMap<String, String>,
        clientOutgoingHeaders: MultivaluedMap<String, String>
    ): MultivaluedMap<String, String> {
        clientOutgoingHeaders.add(X_API_KEY, API_KEY)
        return clientOutgoingHeaders
    }
}
