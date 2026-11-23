/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.brp.log

import nl.info.zac.log.LogEvent
import java.util.UUID

class BrpValueResolutionFailed(
    val valueDescription: String,
    val defaultValue: String?,
    val zaaktypeUuid: UUID
) : LogEvent("Failed to resolve BRP value for zaaktype; using the default value")
