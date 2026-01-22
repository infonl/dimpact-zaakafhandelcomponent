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

fun authenticate(testUser: TestUser) = authenticate(
    username = testUser.username,
    password = testUser.password
)

/**
 * Logs out the given user in ZAC (and therefore also in Keycloak).
 */
fun logout(testUser: TestUser, refreshToken: String) {
    logger.info { "Logging out user: '${testUser.username}'" }
    // to logout from Keycloak directly (which is a legacy and unrecommended flow) we need to include
    // both the refresh token and client credentials
    // Ssee: https://www.keycloak.org/securing-apps/oidc-layers
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

private fun authenticate(username: String, password: String): Pair<String, String> {
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
