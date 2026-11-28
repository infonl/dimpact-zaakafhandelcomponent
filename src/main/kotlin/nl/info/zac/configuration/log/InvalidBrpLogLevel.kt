/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.configuration.log

import nl.info.zac.log.LogEvent

class InvalidBrpLogLevel(
    val logLevel: String?
) : LogEvent("Invalid BRP log level; BRP logging is switched off")
