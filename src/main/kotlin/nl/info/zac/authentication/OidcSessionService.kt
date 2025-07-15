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
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

@Singleton
@AllOpen
@Suppress("LongParameterList")
class OidcSessionService internal constructor(
    private val userPrincipalFilter: UserPrincipalFilter,
    private val httpSession: Instance<HttpSession>,
    private val authServer: String,
    private val authRealm: String,
    private val clientId: String,
    private val clientSecret: String,
    private val connectionFactory: (String) -> HttpURLConnection
) {
    @Inject
    constructor(
        userPrincipalFilter: UserPrincipalFilter,
        httpSession: Instance<HttpSession>,
        @ConfigProperty(name = "AUTH_SERVER") authServer: String,
        @ConfigProperty(name = "AUTH_REALM") authRealm: String,
        @ConfigProperty(name = "AUTH_RESOURCE") clientId: String,
        @ConfigProperty(name = "AUTH_SECRET") clientSecret: String
    ) : this(
        userPrincipalFilter,
        httpSession,
        authServer,
        authRealm,
        clientId,
        clientSecret,
        { url -> URL(url).openConnection() as HttpURLConnection }
    )

    /**
     * Refreshes the OIDC user session using the refresh token stored in the HttpSession.
     * Updates the session with the new refresh token.
     */
    fun refreshUserSession() {
        val refreshToken = httpSession.get().getAttribute("refresh_token") as? String
        check(refreshToken != null) { "No refresh token found in session" }

        val params =
            "grant_type=refresh_token" +
                "&refresh_token=${URLEncoder.encode(refreshToken, "UTF-8")}" +
                "&client_id=${URLEncoder.encode(clientId, "UTF-8")}" +
                "&client_secret=${URLEncoder.encode(clientSecret, "UTF-8")}"

        val keycloakUrl = "$authServer/realms/$authRealm/protocol/openid-connect/token"

        val connection = connectionFactory(keycloakUrl)
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        connection.outputStream.use { it.write(params.toByteArray()) }

        val response = connection.inputStream.bufferedReader().readText()
        if(response.contains("error")) {
            error("Failed to refresh user session: $response")
        }
        val mapper = jacksonObjectMapper()
        val json = mapper.readTree(response)

        val newAccessToken = json["access_token"].asText()
        val newRefreshToken = json["refresh_token"].asText()
        httpSession.get().setAttribute("refresh_token", newRefreshToken)

        val newOidcPrincipal = userPrincipalFilter.createOidcPrincipalFromAccessToken(newAccessToken)
        userPrincipalFilter.setLoggedInUserOnHttpSession(newOidcPrincipal, httpSession.get())
    }
}
