/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.officeconverter

import jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION
import jakarta.ws.rs.core.MultivaluedMap
import org.eclipse.microprofile.config.ConfigProvider
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory
import java.util.Base64

private fun configuredValue(key: String) = ConfigProvider.getConfig().getValue(key, String::class.java)

/**
 * Adds basic authentication credentials to every office converter request. The credentials are required
 * configuration, so ZAC fails to start rather than falling back to unauthenticated requests.
 */
class OfficeConverterClientHeadersFactory(
    username: String = configuredValue("office.converter.username"),
    password: String = configuredValue("office.converter.password")
) : ClientHeadersFactory {
    private val basicAuthentication =
        "Basic " + Base64.getEncoder().encodeToString("$username:$password".toByteArray())

    override fun update(
        incomingHeaders: MultivaluedMap<String, String>,
        clientOutgoingHeaders: MultivaluedMap<String, String>
    ): MultivaluedMap<String, String> {
        clientOutgoingHeaders.add(AUTHORIZATION, basicAuthentication)
        return clientOutgoingHeaders
    }
}
