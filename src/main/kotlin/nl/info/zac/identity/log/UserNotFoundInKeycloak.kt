/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.identity.log

import nl.info.zac.log.LogEvent

// Without the employee: Keycloak does not know the user, so its Keycloak user ID cannot be looked up either
class UserNotFoundInKeycloak(
    val context: String?
) : LogEvent("User could not be found in Keycloak; returning the username only")
