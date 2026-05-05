/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.brp.util

import jakarta.inject.Inject
import jakarta.ws.rs.core.MultivaluedMap
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory

class BrpClientHeadersFactory @Inject constructor(
    private val brpProtocolleringContext: BrpProtocolleringContext,
) : ClientHeadersFactory {
    override fun update(
        incomingHeaders: MultivaluedMap<String, String>,
        clientOutgoingHeaders: MultivaluedMap<String, String>
    ): MultivaluedMap<String, String> {
        brpProtocolleringContext.headers.forEach { (h, v) ->
            clientOutgoingHeaders.addHeader(h, v)
        }
        return clientOutgoingHeaders
    }

    private fun MultivaluedMap<String, String>.addHeader(name: String, value: String?) {
        if (value != null && !containsKey(name)) add(name, value)
    }
}
