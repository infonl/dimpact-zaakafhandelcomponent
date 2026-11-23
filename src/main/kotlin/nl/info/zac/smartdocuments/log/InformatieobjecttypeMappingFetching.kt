/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.smartdocuments.log

import nl.info.zac.log.LogEvent
import java.util.UUID

class InformatieobjecttypeMappingFetching(
    val zaaktypeUuid: UUID,
    val templateGroupId: String,
    val templateId: String
) : LogEvent("Fetching information object type UUID mapping")
