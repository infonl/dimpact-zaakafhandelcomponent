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
import java.util.Optional
import kotlin.jvm.optionals.getOrDefault

class BRPClientHeadersFactory @Inject constructor(
    @ConfigProperty(name = "brp.api.key")
    private val apiKey: Optional<String>,

    @ConfigProperty(name = "brp.protocolering")
    private val protocoleringEnabled: Optional<Boolean>,

    @ConfigProperty(name = "brp.origin.oin")
    private val originOIN: Optional<String>,

    @ConfigProperty(name = "brp.doelbinding")
    private val purpose: Optional<String>,

    @ConfigProperty(name = "brp.verwerking")
    private val process: Optional<String>,

    @Inject
    private var loggedInUserInstance: Instance<LoggedInUser>,
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
    ): MultivaluedMap<String, String> =
        if (protocoleringEnabled.getOrDefault(false)) {
            clientOutgoingHeaders.apply {
                createHeader(X_API_KEY, apiKey)
                createHeader(X_ORIGIN_OIN, originOIN)
                createHeader(X_DOELBINDING, purpose)
                createHeader(X_VERWERKING, process)
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
