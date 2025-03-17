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
import java.util.logging.Level
import java.util.logging.Logger

class BRPClientHeadersFactory @Inject constructor(
    @ConfigProperty(name = "brp.api.key")
    private val apiKey: Optional<String> = Optional.empty(),

    @ConfigProperty(name = "brp.origin.oin")
    private val originOIN: Optional<String> = Optional.empty(),

    @ConfigProperty(name = "brp.doelbinding")
    private val doelbinding: Optional<String> = Optional.empty(),

    @ConfigProperty(name = "brp.verwerking")
    private val verwerking: Optional<String> = Optional.empty(),

    @Inject
    private var loggedInUserInstance: Instance<LoggedInUser>
) : ClientHeadersFactory {

    companion object {
        private const val X_API_KEY = "X-API-KEY"
        private const val X_ORIGIN_OIN = "X-ORIGIN-OIN"
        private const val X_DOELBINDING = "X-DOELBINDING"
        private const val X_VERWERKING = "X-VERWERKING"
        private const val X_GEBRUIKER = "X-GEBRUIKER"

        private const val SYSTEM_USER = "systeem"

        private val LOG = Logger.getLogger(BRPClientHeadersFactory::class.java.name)
    }

    override fun update(
        incomingHeaders: MultivaluedMap<String, String>,
        clientOutgoingHeaders: MultivaluedMap<String, String>
    ): MultivaluedMap<String, String> {
        if (apiKey.isPresent) {
            clientOutgoingHeaders.add(X_API_KEY, apiKey.get())
        }

        if (originOIN.isPresent) {
            clientOutgoingHeaders.add(X_ORIGIN_OIN, originOIN.get())
        }
        if (doelbinding.isPresent) {
            clientOutgoingHeaders.add(X_DOELBINDING, doelbinding.get())
        }
        if (verwerking.isPresent) {
            clientOutgoingHeaders.add(X_VERWERKING, verwerking.get())
        }
        clientOutgoingHeaders.add(X_GEBRUIKER, getUser())

        return clientOutgoingHeaders
    }

    private fun getUser(): String =
        try {
            loggedInUserInstance.get().id
        } catch (urException: UnsatisfiedResolutionException) {
            LOG.log(Level.WARNING, "No logged in user found!", urException.message)
            SYSTEM_USER
        }
}
