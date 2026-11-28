/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.brp.log

import nl.info.zac.log.LogEvent
import java.util.UUID

class BrpProtocolleringValueNotDetermined(
    val header: String,
    val zaaktypeUuid: UUID?
) : LogEvent("BRP protocollering value could not be determined and no default value has been configured")
