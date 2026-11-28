/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.notification.log

import nl.info.zac.log.LogEvent
import java.util.UUID

class FormerBehandelaarSignaleringenDeleted(
    val zaakUuid: UUID,
    val signaleringCount: Int
) : LogEvent("Deleted zaak signaleringen of former behandelaren")
