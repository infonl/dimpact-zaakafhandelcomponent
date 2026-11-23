/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.sensitive.log

import nl.info.zac.log.LogEvent
import java.util.UUID

class SensitiveDataRemoved(
    val key: UUID?,
    val cause: String
) : LogEvent("Removing sensitive storage data")
