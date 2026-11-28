/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.smartdocuments.log

import nl.info.zac.log.LogEvent
import java.util.UUID

class TemplateMappingDeleted(
    val zaaktypeUuid: UUID,
    val deletedCount: Int
) : LogEvent("Deleted template mapping for zaaktype")
