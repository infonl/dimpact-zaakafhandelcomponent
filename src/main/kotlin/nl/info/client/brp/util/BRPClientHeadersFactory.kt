/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.brp.util

import jakarta.enterprise.inject.Instance
import jakarta.enterprise.inject.UnsatisfiedResolutionException
import jakarta.inject.Inject
import jakarta.ws.rs.core.MultivaluedMap
import nl.info.client.brp.util.BRPClientHeadersFactory.Companion.ICONNECT_AUDIT_LOG_PROVIDER
import nl.info.zac.authentication.LoggedInUser
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory
import java.util.Optional
import kotlin.jvm.optionals.getOrDefault

class BRPClientHeadersFactory @Inject constructor(
    @ConfigProperty(name = "brp.api.key")
    private val apiKey: Optional<String>,

    @ConfigProperty(name = "brp.origin.oin")
    private val originOIN: Optional<String>,

    @ConfigProperty(name = "brp.protocolering.aanbieder")
    private val auditLogProvider: Optional<String>,

    @Inject
    private var loggedInUserInstance: Instance<LoggedInUser>,
) : ClientHeadersFactory {

    companion object {
        const val ICONNECT_X_DOELBINDING = "X-DOELBINDING"
        const val ICONNECT_X_VERWERKING = "X-VERWERKING"
        private const val ICONNECT_X_API_KEY = "X-API-KEY"
        private const val ICONNECT_X_ORIGIN_OIN = "X-ORIGIN-OIN"
        private const val ICONNECT_X_GEBRUIKER = "X-GEBRUIKER"

        private const val TWOSECURE_X_REQUEST_APPLICATION = "X-REQUEST-APPLICATION"
        private const val TWOSECURE_X_REQUEST_ORGANIZATION = "X-REQUEST-ORGANIZATION"
        const val TWOSECURE_X_REQUEST_AFNEMERSCODE = "X-REQUEST-AFNEMERSCODE"
        private const val TWOSECURE_X_REQUEST_USER = "X-REQUEST-USER"

        private const val SYSTEM_USER = "BurgerZelf"
        const val ICONNECT_AUDIT_LOG_PROVIDER = "iConnect"
    }

    override fun update(
        incomingHeaders: MultivaluedMap<String, String>,
        clientOutgoingHeaders: MultivaluedMap<String, String>
    ): MultivaluedMap<String, String> =
        if (originOIN.isPresent) {
            if (auditLogProvider.matchesDefault(ICONNECT_AUDIT_LOG_PROVIDER)) {
                clientOutgoingHeaders.apply {
                    createHeader(ICONNECT_X_API_KEY, apiKey)
                    createHeader(ICONNECT_X_ORIGIN_OIN, originOIN)
                    createHeader(ICONNECT_X_GEBRUIKER, getUser())
                }
            } else {
                clientOutgoingHeaders.apply {
                    createHeader(TWOSECURE_X_REQUEST_APPLICATION, apiKey)
                    createHeader(TWOSECURE_X_REQUEST_ORGANIZATION, originOIN)
                    createHeader(TWOSECURE_X_REQUEST_USER, getUser())
                }
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

fun Optional<String>.matchesDefault(defaultValue: String) = getOrDefault(defaultValue) == defaultValue
