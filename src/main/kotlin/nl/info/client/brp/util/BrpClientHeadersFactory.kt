/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.brp.util

import jakarta.enterprise.inject.Instance
import jakarta.enterprise.inject.UnsatisfiedResolutionException
import jakarta.inject.Inject
import jakarta.ws.rs.core.MultivaluedMap
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.configuratie.BrpConfiguration
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory
import java.util.Optional

class BrpClientHeadersFactory @Inject constructor(
    private val brpConfiguration: BrpConfiguration,
    private var loggedInUserInstance: Instance<LoggedInUser>,
) : ClientHeadersFactory {

    companion object {
        const val X_DOELBINDING = "X-DOELBINDING"
        const val X_VERWERKING = "X-VERWERKING"
        const val X_API_KEY = "X-API-KEY"
        const val X_ORIGIN_OIN = "X-ORIGIN-OIN"
        const val X_GEBRUIKER = "X-GEBRUIKER"

        const val SYSTEM_USER = "BurgerZelf"
    }

    override fun update(
        incomingHeaders: MultivaluedMap<String, String>,
        clientOutgoingHeaders: MultivaluedMap<String, String>
    ): MultivaluedMap<String, String> =
        if (brpConfiguration.isBrpProtocoleringEnabled()) {
            clientOutgoingHeaders.apply {
                createHeader(X_API_KEY, brpConfiguration.apiKey)
                createHeader(X_ORIGIN_OIN, brpConfiguration.originOIN)
                createHeader(X_GEBRUIKER, getUser())
            }
        } else {
            clientOutgoingHeaders
        }

    private fun MultivaluedMap<String, String>.createHeader(name: String, value: Optional<String>) {
        if (value.isPresent) {
            createHeader(name, value.get())
        }
    }

    private fun MultivaluedMap<String, String>.createHeader(name: String, value: String) {
        if (!containsKey(name)) {
            add(name, value)
        }
    }

    private fun getUser(): String =
        try {
            loggedInUserInstance.get().id
        } catch (_: UnsatisfiedResolutionException) {
            SYSTEM_USER
        }
}
