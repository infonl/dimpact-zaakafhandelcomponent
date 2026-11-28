/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.zaak.log

import nl.info.zac.log.Employee
import nl.info.zac.log.LogEvent

class EmployeeNotInGroup(
    val assignee: Employee,
    val groupId: String,
    val zaakCount: Int,
    val screenEventResourceId: String?
) : LogEvent("Employee is not in the group; skipping all zaken")
