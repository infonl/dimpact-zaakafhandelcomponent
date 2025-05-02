/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.authentication

import jakarta.inject.Inject
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.config.inject.ConfigProperty

/**
 * Filter to secure internal endpoints with API key authentication.
 * To access these internal ZAC endpoints the API key needs to be passed in the "X-API-KEY" request header.
 * If the API key does not match the configured key, a 401 Unauthorized response is returned.
 */
@Provider
@InternalEndpoint
@NoArgConstructor
class ZacApiKeyAuthFilter @Inject constructor(
    @ConfigProperty(name = "ZAC_INTERNAL_ENDPOINTS_API_KEY")
    private val zacInternalEndpointsApiKey: String
) : ContainerRequestFilter {
    companion object {
        private const val API_KEY_HEADER = "X-API-KEY"
    }

    override fun filter(requestContext: ContainerRequestContext) {
        val apiKey = requestContext.getHeaderString(API_KEY_HEADER)
        if (apiKey != zacInternalEndpointsApiKey) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build())
        }
    }
}
