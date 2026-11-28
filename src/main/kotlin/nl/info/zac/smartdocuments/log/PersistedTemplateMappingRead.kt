/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.smartdocuments.log

import nl.info.zac.log.LogEvent
import java.util.UUID

class PersistedTemplateMappingRead(
    val zaaktypeUuid: UUID,
    val source: TemplateMappingSource
) : LogEvent("Reading the persisted template mapping for zaaktype")
