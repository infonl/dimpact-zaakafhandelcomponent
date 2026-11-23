/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.brp.log

import nl.info.zac.log.LogEvent

class BrpValueNotAscii(
    val valueDescription: String,
    val defaultValue: String?
) : LogEvent("Resolved BRP value contains non-ASCII characters; using the default value")
