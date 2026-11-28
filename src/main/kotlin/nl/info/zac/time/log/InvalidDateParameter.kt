/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.time.log

import nl.info.zac.log.LogEvent

class InvalidDateParameter(
    val targetType: String
) : LogEvent("Could not parse date parameter")
