/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.keycloak.log

import nl.info.zac.log.LogEvent

class KeycloakAdminClientConfigured(
    val url: String,
    val realm: String,
    val clientId: String
) : LogEvent("Keycloak admin client configured")
