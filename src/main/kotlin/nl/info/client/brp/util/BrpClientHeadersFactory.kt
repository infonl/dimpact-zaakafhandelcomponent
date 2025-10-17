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
import nl.info.zac.authentication.SecurityUtil.Companion.FUNCTIONEEL_GEBRUIKER
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

        // Used when no logged in user is available. According to iConnect documentation:
        //
        // >> Wordt de aanroep van de BRPv2-API getriggerd door een systeemfunctie (bv. na ontvangst van
        // een gebeurtenisnotificatie), dan als gebruiker ‘Systeem’ te vullen.
        const val SYSTEM_USER = "Systeem"

        // Audit log headers are limited to 242 characters as this is the maximum length of the DB columns:
        // https://github.com/VNG-Realisatie/gemma-verwerkingenlogging/blob/002df5b01bf7d10142c9ae042a041b096989ced9/docs/api-write/oas-specification/logging-verwerkingen-api/openapi.yaml#L1170-L1175
        const val MAX_HEADER_SIZE = 242

        // User headers are limited to 40 characters as this is the maximum length of the DB column:
        // https://github.com/VNG-Realisatie/gemma-verwerkingenlogging/blob/002df5b01bf7d10142c9ae042a041b096989ced9/docs/api-write/oas-specification/logging-verwerkingen-api/openapi.yaml#L1224-L1229
        const val MAX_USER_HEADER_SIZE = 40
    }

    override fun update(
        incomingHeaders: MultivaluedMap<String, String>,
        clientOutgoingHeaders: MultivaluedMap<String, String>
    ): MultivaluedMap<String, String> =
        if (brpConfiguration.isBrpProtocolleringEnabled()) {
            clientOutgoingHeaders.apply {
                createHeader(X_API_KEY, brpConfiguration.apiKey)
                createHeader(X_ORIGIN_OIN, brpConfiguration.originOIN)
                createHeader(X_GEBRUIKER, getUser())
            }
        } else {
            clientOutgoingHeaders
        }.trimToMaxSize()

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

    private fun MultivaluedMap<String, String>.trimToMaxSize() =
        onEach { keyValuePair ->
            val maxSize = if (keyValuePair.key == X_GEBRUIKER) MAX_USER_HEADER_SIZE else MAX_HEADER_SIZE
            keyValuePair.value?.let { valuesList ->
                valuesList.onEachIndexed { index, value ->
                    valuesList[index] = value.take(maxSize)
                }
            }
        }

    private fun getUser(): String =
        try {
            loggedInUserInstance.get().id.takeIf { it != FUNCTIONEEL_GEBRUIKER.id } ?: SYSTEM_USER
        } catch (_: UnsatisfiedResolutionException) {
            SYSTEM_USER
        }
}
