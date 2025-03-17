/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.brp.util

import jakarta.enterprise.inject.Instance
import jakarta.enterprise.inject.UnsatisfiedResolutionException
import jakarta.inject.Inject
import jakarta.ws.rs.core.MultivaluedMap
import nl.info.zac.authentication.LoggedInUser
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory

class BRPClientHeadersFactory @Inject constructor(
    @ConfigProperty(name = "brp.api.key")
    private val apiKey: String,

    @ConfigProperty(name = "brp.origin.oin")
    private val originOIN: String,

    @ConfigProperty(name = "brp.doelbinding.default")
    private val purpose: String,

    @ConfigProperty(name = "brp.verwerking.default")
    private val process: String,

    @Inject
    private var loggedInUserInstance: Instance<LoggedInUser>
) : ClientHeadersFactory {

    companion object {
        private const val X_API_KEY = "X-API-KEY"
        private const val X_ORIGIN_OIN = "X-ORIGIN-OIN"
        private const val X_DOELBINDING = "X-DOELBINDING"
        private const val X_VERWERKING = "X-VERWERKING"
        private const val X_GEBRUIKER = "X-GEBRUIKER"

        private const val SYSTEM_USER = "BurgerZelf"
    }

    override fun update(
        incomingHeaders: MultivaluedMap<String, String>,
        clientOutgoingHeaders: MultivaluedMap<String, String>
    ): MultivaluedMap<String, String> {
        clientOutgoingHeaders.add(X_API_KEY, apiKey)
        clientOutgoingHeaders.add(X_ORIGIN_OIN, originOIN)
        clientOutgoingHeaders.add(X_DOELBINDING, purpose)
        clientOutgoingHeaders.add(X_VERWERKING, process)
        clientOutgoingHeaders.add(X_GEBRUIKER, getUser())

        return clientOutgoingHeaders
    }

    private fun getUser(): String =
        try {
            loggedInUserInstance.get().id
        } catch (_: UnsatisfiedResolutionException) {
            SYSTEM_USER
        }
}
