/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.ztc.log

import nl.info.zac.log.LogEvent
import java.util.UUID

class BesluittypeReadFailed(
    val besluittypeUuid: UUID
) : LogEvent("Failed to read besluittype, so all besluittype lists are removed from the cache")
