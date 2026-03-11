/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
@file:Suppress("PackageName")

package nl.info.client.or.`object`

import jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION
import jakarta.ws.rs.core.MultivaluedMap
import org.eclipse.microprofile.config.ConfigProvider
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory

class ObjectsClientHeadersFactory : ClientHeadersFactory {
    companion object {
        private val TOKEN = ConfigProvider.getConfig().getValue("objects.api.token", String::class.java)
    }

    override fun update(
        incomingHeaders: MultivaluedMap<String, String>,
        clientOutgoingHeaders: MultivaluedMap<String, String>
    ): MultivaluedMap<String, String> {
        clientOutgoingHeaders.add(AUTHORIZATION, "Token $TOKEN")
        return clientOutgoingHeaders
    }
}
