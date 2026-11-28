/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util.log

import nl.info.zac.log.LogEvent

class PolicyModuleDeploymentStarted(
    val moduleId: String
) : LogEvent("Policy module deployment started")
