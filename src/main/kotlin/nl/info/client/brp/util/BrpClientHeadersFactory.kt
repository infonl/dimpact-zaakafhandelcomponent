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
import nl.info.zac.authentication.LoggedInUserProvider.Companion.FUNCTIONEEL_GEBRUIKER
import nl.info.zac.configuration.BrpConfiguration
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory

class BrpClientHeadersFactory @Inject constructor(
    private val brpConfiguration: BrpConfiguration,
    private val loggedInUserInstance: Instance<LoggedInUser>,
    private val brpProtocolleringContext: BrpProtocolleringContext,
) : ClientHeadersFactory {

    companion object {
        // Used when no logged in user is available. According to iConnect documentation:
        //
        // >> Wordt de aanroep van de BRPv2-API getriggerd door een systeemfunctie (bv. na ontvangst van
        // een gebeurtenisnotificatie), dan als gebruiker 'Systeem' te vullen.
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
    ): MultivaluedMap<String, String> {
        if (brpConfiguration.isBrpProtocolleringEnabled()) {
            clientOutgoingHeaders.apply {
                brpConfiguration.getHeaderNameOriginOin()?.let { addHeader(it, brpConfiguration.getOriginOIN()) }
                brpConfiguration.getHeaderNameGebruiker()?.let { addHeader(it, getUser()) }
                brpConfiguration.getHeaderNameDoelbinding()?.let { name ->
                    brpProtocolleringContext.doelbinding?.let { addHeader(name, it) }
                }
                brpConfiguration.getHeaderNameVerwerking()?.let { name ->
                    brpProtocolleringContext.verwerking?.let { addHeader(name, it) }
                }
                brpConfiguration.getHeaderNameToepassing()?.let { name ->
                    brpConfiguration.getToepassing()?.let { addHeader(name, it) }
                }
            }
        }
        return clientOutgoingHeaders.trimHeadersToMaxSize()
    }

    private fun MultivaluedMap<String, String>.addHeader(name: String, value: String?) {
        if (value != null && !containsKey(name)) add(name, value)
    }

    private fun MultivaluedMap<String, String>.trimHeadersToMaxSize(): MultivaluedMap<String, String> {
        val gebruikerHeaderName = brpConfiguration.getHeaderNameGebruiker()
        return onEach { (key, values) ->
            val maxSize = if (key == gebruikerHeaderName) MAX_USER_HEADER_SIZE else MAX_HEADER_SIZE
            values?.replaceAll { it.take(maxSize) }
        }
    }

    private fun getUser(): String =
        try {
            loggedInUserInstance.get().id.takeIf { it != FUNCTIONEEL_GEBRUIKER.id } ?: SYSTEM_USER
        } catch (_: UnsatisfiedResolutionException) {
            SYSTEM_USER
        }
}
