/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.log

import nl.info.zac.log.LogEvent
import java.util.UUID

class DocumentCreationFailed(
    val zaakUuid: UUID,
    val taakId: String?
) : LogEvent("Failed to create document for zaak")
