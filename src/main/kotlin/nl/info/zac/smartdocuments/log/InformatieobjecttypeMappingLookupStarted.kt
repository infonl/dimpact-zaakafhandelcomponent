/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.smartdocuments.log

import nl.info.zac.log.LogEvent
import java.util.UUID

class InformatieobjecttypeMappingLookupStarted(
    val zaaktypeUuid: UUID,
    val templateGroupId: String,
    val templateId: String
) : LogEvent("Information object type UUID mapping lookup started")
