/*
 * SPDX-FileCopyrightText: 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.kvk.util

import jakarta.ws.rs.core.MultivaluedMap
import org.eclipse.microprofile.config.ConfigProvider
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory

class KvkClientHeadersFactory : ClientHeadersFactory {
    override fun update(
        incomingHeaders: MultivaluedMap<String?, String?>?,
        clientOutgoingHeaders: MultivaluedMap<String?, String?>
    ): MultivaluedMap<String?, String?> {
        clientOutgoingHeaders.add(KVK_API_KEY_HEADER_FIELD, API_KEY)
        return clientOutgoingHeaders
    }

    companion object {
        const val KVK_API_KEY_HEADER_FIELD: String = "apikey"

        val API_KEY: String? = ConfigProvider.getConfig().getValue<String?>(
            "kvk.api.key",
            String::class.java
        )
    }
}
