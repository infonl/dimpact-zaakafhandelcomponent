/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.klant.log

import nl.info.zac.log.LogEvent
import java.util.UUID

class BetrokkeneNotFound(
    val betrokkeneUuid: UUID?
) : LogEvent("Failed to find betrokkene")
