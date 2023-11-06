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
    fun requestAccessTokenFromKeycloak() =
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
        ).jsonObject.getString("access_token")
}
