/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest.client

import khttp.requests.GenericRequest
import nl.lifely.zac.itest.config.ItestConfiguration.KEYCLOAK_CLIENT
import nl.lifely.zac.itest.config.ItestConfiguration.KEYCLOAK_CLIENT_SECRET
import nl.lifely.zac.itest.config.ItestConfiguration.KEYCLOAK_HOSTNAME_URL
import nl.lifely.zac.itest.config.ItestConfiguration.KEYCLOAK_REALM

class KeycloakClient {
    private lateinit var refreshToken: String

    fun authenticate() =
        khttp.post(
            url = "$KEYCLOAK_HOSTNAME_URL/realms/$KEYCLOAK_REALM/protocol/openid-connect/token",
            headers = GenericRequest.DEFAULT_FORM_HEADERS,
            data = mapOf(
                "client_id" to KEYCLOAK_CLIENT,
                "grant_type" to "password",
                "username" to "testuser1",
                "password" to "testuser1",
                "client_secret" to KEYCLOAK_CLIENT_SECRET
            )
        ).apply {
            refreshToken = jsonObject.getString("refresh_token")
        }

    /**
     * Always request a new access token using the current refresh token to make sure we remain
     * authenticated and our access token does not expire.
     * Note that this assumes that subsequent calls to this method are done quickly enough so that the
     * refresh token does not expire in the meantime.
     */
    fun requestAccessToken(): String {
        khttp.post(
            url = "$KEYCLOAK_HOSTNAME_URL/realms/$KEYCLOAK_REALM/protocol/openid-connect/token",
            headers = GenericRequest.DEFAULT_FORM_HEADERS,
            data = mapOf(
                "client_id" to KEYCLOAK_CLIENT,
                "grant_type" to "refresh_token",
                "refresh_token" to refreshToken,
                "client_secret" to KEYCLOAK_CLIENT_SECRET
            )
        ).apply {
            // a new refresh token is optional, so only update it if it is present
            if (jsonObject.has("refresh_token"))
                refreshToken = jsonObject.getString("refresh_token")
            return jsonObject.getString("access_token")
        }
    }
}

