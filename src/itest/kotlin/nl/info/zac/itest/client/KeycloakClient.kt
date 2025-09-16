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
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_1_PASSWORD
import nl.info.zac.itest.config.ItestConfiguration.TEST_USER_1_USERNAME
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

lateinit var refreshToken: String
private val okHttpClient = OkHttpClient.Builder().build()
private val logger = KotlinLogging.logger {}

private const val ACCESS_TOKEN_ATTRIBUTE = "access_token"
private const val REFRESH_TOKEN_ATTRIBUTE = "refresh_token"

fun authenticate(
    username: String = TEST_USER_1_USERNAME,
    password: String = TEST_USER_1_PASSWORD
) = okHttpClient.newCall(
    Request.Builder()
        .headers(Headers.headersOf("Content-Type", "application/x-www-form-urlencoded"))
        .header("Authorization", Credentials.basic(KEYCLOAK_CLIENT, KEYCLOAK_CLIENT_SECRET))
        .url("$KEYCLOAK_HOSTNAME_URL/realms/$KEYCLOAK_REALM/protocol/openid-connect/token")
        .post(
            FormBody.Builder()
                .add("grant_type", "password")
                .add("username", username)
                .add("password", password)
                .build()
        )
        .build()
).execute().apply {
    logger.info { "--- authenticate status code: $code ---" }
    refreshToken = JSONObject(this.body.string()).getString(REFRESH_TOKEN_ATTRIBUTE)
}

/**
 * Always request a new access token using the current refresh token to make sure we remain
 * authenticated and our access token does not expire.
 */
fun refreshAccessToken(): String {
    okHttpClient.newCall(
        Request.Builder()
            .headers(Headers.headersOf("Content-Type", "application/x-www-form-urlencoded"))
            .header("Authorization", Credentials.basic(KEYCLOAK_CLIENT, KEYCLOAK_CLIENT_SECRET))
            .url("$KEYCLOAK_HOSTNAME_URL/realms/$KEYCLOAK_REALM/protocol/openid-connect/token")
            .post(
                FormBody.Builder()
                    .add("grant_type", "refresh_token")
                    .add("refresh_token", refreshToken)
                    .build()
            )
            .build()
    ).execute().apply {
        with(JSONObject(this.body.string())) {
            if (has(REFRESH_TOKEN_ATTRIBUTE)) {
                refreshToken = getString(REFRESH_TOKEN_ATTRIBUTE)
            }
            return getString(ACCESS_TOKEN_ATTRIBUTE)
        }
    }
}
