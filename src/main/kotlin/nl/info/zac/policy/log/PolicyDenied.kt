/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.log

import nl.info.zac.log.Employee
import nl.info.zac.log.LogEvent

class PolicyDenied(
    val action: String,
    val zaakIdentificatie: String?,
    val employee: Employee
) : LogEvent("Policy denied")
