/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.client

import io.github.oshai.kotlinlogging.KotlinLogging
import nl.info.zac.itest.config.ItestConfiguration.KEYCLOAK_CLIENT
import nl.info.zac.itest.config.ItestConfiguration.KEYCLOAK_CLIENT_SECRET
import nl.info.zac.itest.config.ItestConfiguration.KEYCLOAK_HOSTNAME_URL
import nl.info.zac.itest.config.ItestConfiguration.KEYCLOAK_REALM
import nl.info.zac.itest.config.TestUser
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

private val okHttpClient = OkHttpClient.Builder().build()
private val logger = KotlinLogging.logger {}

private const val ACCESS_TOKEN_ATTRIBUTE = "access_token"
private const val REFRESH_TOKEN_ATTRIBUTE = "refresh_token"

/**
 * Authenticates the given user in Keycloak and returns a pair of access token and refresh token.
 * Note that this does not log in the user to ZAC, and therefore, no user session is created in ZAC (only in Keycloak) in this function.
 * However, the returned access token can now be used to access protected endpoints in ZAC on behalf of the user.
 * On the first request to ZAC with this token, a user session will be created in ZAC.
 *
 * This function should normally only be called from [ItestHttpClient] and not directly from our integration tests.
 *
 * @param testUser The user to authenticate.
 * @return A pair containing the access token and refresh token.
 */
fun authenticate(testUser: TestUser): Pair<String, String> {
    val username = testUser.username
    val password = testUser.password
    logger.info { "Authenticating: '$username'" }
    val request = Request.Builder()
        .url("$KEYCLOAK_HOSTNAME_URL/realms/$KEYCLOAK_REALM/protocol/openid-connect/token")
        .header("Content-Type", "application/x-www-form-urlencoded")
        .post(
            FormBody.Builder()
                .add("client_id", KEYCLOAK_CLIENT)
                .add("client_secret", KEYCLOAK_CLIENT_SECRET)
                .add("grant_type", "password")
                .add("username", username)
                .add("password", password)
                .build()
        )
        .build()
    return okHttpClient.newCall(request).execute().use { response ->
        with(JSONObject(response.body.string())) {
            Pair(
                getString(ACCESS_TOKEN_ATTRIBUTE),
                getString(REFRESH_TOKEN_ATTRIBUTE)
            )
        }
    }
}

/**
 * Logs out the given user in Keycloak, using the provided refresh token.
 * Note that this function does not log out the user from ZAC itself, only from Keycloak.
 * Therefore, the user's session in ZAC, if it exists, will remain intact.
 * However, that ZAC user session will no longer be valid.
 *
 * We log out directly in Keycloak using OIDC, similar to how ZAC does this using the WildFly Elytron framework.
 * This is considered a legacy flow in Keycloak.
 * It requires us to include both the refresh token and client credentials in the logout request.
 * See: [https://www.keycloak.org/securing-apps/oidc-layers].
 *
 * @param testUser The user to log out.
 * @param refreshToken The refresh token of the user.
 */
fun logout(testUser: TestUser, refreshToken: String) {
    logger.info { "Logging out user: '${testUser.username}'" }
    val request = Request.Builder()
        .url("$KEYCLOAK_HOSTNAME_URL/realms/$KEYCLOAK_REALM/protocol/openid-connect/logout")
        .header("Content-Type", "application/x-www-form-urlencoded")
        .post(
            FormBody.Builder()
                .add("client_id", KEYCLOAK_CLIENT)
                .add("client_secret", KEYCLOAK_CLIENT_SECRET)
                .add(REFRESH_TOKEN_ATTRIBUTE, refreshToken)
                .build()
        )
        .build()
    okHttpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
            logger.warn { "Logout request for user '${testUser.username}' failed with code: ${response.code}" }
        } else {
            logger.info { "User '${testUser.username}' logged out successfully." }
        }
    }
}
