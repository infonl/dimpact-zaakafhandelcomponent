/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest.client

import nl.lifely.zac.itest.config.ItestConfiguration.KEYCLOAK_CLIENT
import nl.lifely.zac.itest.config.ItestConfiguration.KEYCLOAK_CLIENT_SECRET
import nl.lifely.zac.itest.config.ItestConfiguration.KEYCLOAK_HOSTNAME_URL
import nl.lifely.zac.itest.config.ItestConfiguration.KEYCLOAK_REALM
import okhttp3.FormBody
import okhttp3.Headers
import org.json.JSONObject

object KeycloakClient {
    private const val ACCESS_TOKEN_ATTRIBUTE = "access_token"
    private const val REFRESH_TOKEN_ATTRIBUTE = "refresh_token"

    private val zacClient = ZacClient()
    private lateinit var refreshToken: String

    fun authenticate() = zacClient.performPostRequest(
        url = "$KEYCLOAK_HOSTNAME_URL/realms/$KEYCLOAK_REALM/protocol/openid-connect/token",
        headers = Headers.headersOf("Content-Type", "application/x-www-form-urlencoded"),
        requestBody = FormBody.Builder()
            .add("client_id", KEYCLOAK_CLIENT)
            .add("grant_type", "password")
            .add("username", "testuser1")
            .add("password", "testuser1")
            .add("client_secret", KEYCLOAK_CLIENT_SECRET)
            .build()
    ).apply {
        refreshToken = JSONObject(this.body!!.string()).getString(REFRESH_TOKEN_ATTRIBUTE)
    }

    /**
     * Always request a new access token using the current refresh token to make sure we remain
     * authenticated and our access token does not expire.
     * Note that this assumes that subsequent calls to this method are done quickly enough so that the
     * refresh token does not expire in the meantime.
     */
    fun requestAccessToken(): String {
        zacClient.performPostRequest(
            url = "$KEYCLOAK_HOSTNAME_URL/realms/$KEYCLOAK_REALM/protocol/openid-connect/token",
            headers = Headers.headersOf("Content-Type", "application/x-www-form-urlencoded"),
            requestBody = FormBody.Builder()
                .add("client_id", KEYCLOAK_CLIENT)
                .add("grant_type", REFRESH_TOKEN_ATTRIBUTE)
                .add(REFRESH_TOKEN_ATTRIBUTE, refreshToken)
                .add("client_secret", KEYCLOAK_CLIENT_SECRET)
                .build()
        ).apply {
            with(JSONObject(this.body!!.string())) {
                if (has(REFRESH_TOKEN_ATTRIBUTE)) {
                    refreshToken = getString(REFRESH_TOKEN_ATTRIBUTE)
                }
                return getString(ACCESS_TOKEN_ATTRIBUTE)
            }
        }
    }
}
