/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.authentication

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.servlet.http.HttpSession
import nl.info.zac.util.AllOpen
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.eclipse.microprofile.config.inject.ConfigProperty

@Singleton
@AllOpen
@Suppress("LongParameterList")
class OidcSessionService @Inject constructor(
    private val userPrincipalFilter: UserPrincipalFilter,
    private val httpSession: Instance<HttpSession>,
    @ConfigProperty(name = "AUTH_SERVER") private val authServer: String,
    @ConfigProperty(name = "AUTH_REALM") private val authRealm: String,
    @ConfigProperty(name = "AUTH_RESOURCE") private val clientId: String,
    @ConfigProperty(name = "AUTH_SECRET") private val clientSecret: String,
    private val okHttpClient: OkHttpClient
) {
    /**
     * Refreshes the OIDC user session using the refresh token stored in the HttpSession.
     * Updates the session with the new refresh token.
     * Throws OidcSessionException on error.
     */
    @Suppress("TooGenericExceptionCaught", "ThrowsCount")
    fun refreshUserSession() {
        val session = httpSession.get()
        val refreshToken = try {
            session.getAttribute(REFRESH_TOKEN_ATTRIBUTE) as? String
        } catch (e: IllegalStateException) {
            throw OidcSessionException("Session is invalid or expired", e)
        }
        check(refreshToken != null) { "No $REFRESH_TOKEN_ATTRIBUTE found in session" }

        val keycloakUrl = "$authServer/realms/$authRealm/protocol/openid-connect/token"

        val formBody = FormBody.Builder()
            .add("grant_type", REFRESH_TOKEN_ATTRIBUTE)
            .add(REFRESH_TOKEN_ATTRIBUTE, refreshToken)
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .build()

        val request = Request.Builder()
            .url(keycloakUrl)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .post(formBody)
            .build()

        val response = okHttpClient.newCall(request).execute()

        response.use { resp ->
            if (!resp.isSuccessful) {
                throw OidcSessionException("Failed to refresh user session: HTTP ${resp.code}")
            }

            val responseBody = resp.body.string()
            val mapper = jacksonObjectMapper()
            val json = mapper.readTree(responseBody)

            val newAccessToken = json[ACCESS_TOKEN_ATTRIBUTE]?.asText()?.takeIf { it.isNotBlank() }
                ?: throw OidcSessionException("Keycloak response missing $ACCESS_TOKEN_ATTRIBUTE")

            val newRefreshToken = json[REFRESH_TOKEN_ATTRIBUTE]?.asText()?.takeIf { it.isNotBlank() }
                ?: throw OidcSessionException("Keycloak response missing $REFRESH_TOKEN_ATTRIBUTE")

            session.setAttribute(REFRESH_TOKEN_ATTRIBUTE, newRefreshToken)

            val newOidcPrincipal = userPrincipalFilter.createOidcPrincipalFromAccessToken(newAccessToken)
            userPrincipalFilter.setLoggedInUserOnHttpSession(newOidcPrincipal, session)
        }
    }
}

class OidcSessionException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
