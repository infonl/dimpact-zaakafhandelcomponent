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
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory
import java.util.Optional

class BrpClientHeadersFactory @Inject constructor(
    @ConfigProperty(name = "brp.api.key")
    private val apiKey: Optional<String>,

    @ConfigProperty(name = "brp.origin.oin")
    private val originOIN: Optional<String>,

    @Inject
    private var loggedInUserInstance: Instance<LoggedInUser>,
) : ClientHeadersFactory {

    companion object {
        const val X_DOELBINDING = "X-DOELBINDING"
        const val X_VERWERKING = "X-VERWERKING"
        private const val X_API_KEY = "X-API-KEY"
        private const val X_ORIGIN_OIN = "X-ORIGIN-OIN"
        private const val X_GEBRUIKER = "X-GEBRUIKER"
        private const val SYSTEM_USER = "BurgerZelf"

        private const val MAX_HEADER_SIZE = 241
    }

    override fun update(
        incomingHeaders: MultivaluedMap<String, String>,
        clientOutgoingHeaders: MultivaluedMap<String, String>
    ): MultivaluedMap<String, String> =
        if (originOIN.isPresent) {
            clientOutgoingHeaders.apply {
                createHeader(X_API_KEY, apiKey)
                createHeader(X_ORIGIN_OIN, originOIN)
                createHeader(X_GEBRUIKER, getUser())
            }
        } else {
            clientOutgoingHeaders
        }.trimToMaxSize(MAX_HEADER_SIZE)

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

    private fun MultivaluedMap<String, String>.trimToMaxSize(maxSize: Int) =
        onEach { keyValuePair ->
            keyValuePair.value?.let { valuesList ->
                valuesList.onEachIndexed { index, value ->
                    valuesList[index] = value.take(maxSize)
                }
            }
        }

    private fun getUser(): String =
        try {
            loggedInUserInstance.get().id
        } catch (_: UnsatisfiedResolutionException) {
            SYSTEM_USER
        }
}
