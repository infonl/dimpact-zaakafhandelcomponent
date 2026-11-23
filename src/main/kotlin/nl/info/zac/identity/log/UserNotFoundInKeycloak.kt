/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.identity.log

import nl.info.zac.log.Employee
import nl.info.zac.log.LogEvent

class UserNotFoundInKeycloak(
    val employee: Employee,
    val context: String?
) : LogEvent("User could not be found in Keycloak; returning the username only")
