package nl.info.zac.authentication

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.servlet.http.HttpSession
import nl.info.zac.util.AllOpen
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

@Singleton
@AllOpen
class OidcSessionService @Inject constructor(
    private val userPrincipalFilter: UserPrincipalFilter,
    @ActiveSession
    private val httpSession: Instance<HttpSession>
) {

    /**
     * Refreshes the OIDC user session using the refresh token stored in the HttpSession.
     * Updates the session with the new refresh token.
     */
    fun refreshUserSession() {
        val refreshToken = httpSession.get().getAttribute("refresh_token") as? String
        check(refreshToken != null) { "No refresh token found in session" }

        val keycloakUrl =
            System.getenv("AUTH_SERVER") +
                "/realms/" +
                System.getenv("AUTH_REALM") +
                "/protocol/openid-connect/token"
        val clientId = System.getenv("AUTH_RESOURCE")
        val clientSecret = System.getenv("AUTH_SECRET")

        val params =
            "grant_type=refresh_token" +
                "&refresh_token=${URLEncoder.encode(refreshToken, "UTF-8")}" +
                "&client_id=${URLEncoder.encode(clientId, "UTF-8")}" +
                "&client_secret=${URLEncoder.encode(clientSecret, "UTF-8")}"

        val url = URI(keycloakUrl).toURL()
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        connection.outputStream.use { it.write(params.toByteArray()) }

        val response = connection.inputStream.bufferedReader().readText()
        val mapper = jacksonObjectMapper()
        val json = mapper.readTree(response)

        val newAccessToken = json["access_token"].asText()
        val newRefreshToken = json["refresh_token"].asText()
        httpSession.get().setAttribute("refresh_token", newRefreshToken)

        val newOidcPrincipal = userPrincipalFilter.createOidcPrincipalFromTokens(newAccessToken)
        userPrincipalFilter.setLoggedInUserOnHttpSession(newOidcPrincipal, httpSession.get())
    }
}
