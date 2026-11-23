/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag.log

import nl.info.zac.log.LogEvent
import java.util.UUID

class ProductaanvraagReceived(
    val productaanvraagObjectUuid: UUID,
    val isAlreadyHandled: Boolean
) : LogEvent("Handling productaanvraag")
