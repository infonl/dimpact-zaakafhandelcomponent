/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.brp.log

import nl.info.zac.log.LogEvent
import java.util.UUID

class BrpValueResolving(
    val valueDescription: String,
    val zaaktypeUuid: UUID
) : LogEvent("Resolving BRP value for zaaktype")
