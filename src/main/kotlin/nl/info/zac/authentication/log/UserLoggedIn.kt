/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.authentication.log

import nl.info.zac.log.Employee
import nl.info.zac.log.LogEvent

@Suppress("LongParameterList")
class UserLoggedIn(
    val employee: Employee,
    val groupIds: Set<String>,
    val roles: Set<String>,
    val applicationRolesPerZaaktype: Map<String, Set<String>>,
    val overallRoles: Set<String>,
    val hasReadApplicationRole: Boolean,
    val previousSessionEmployee: Employee?
) : LogEvent("User logged in")
