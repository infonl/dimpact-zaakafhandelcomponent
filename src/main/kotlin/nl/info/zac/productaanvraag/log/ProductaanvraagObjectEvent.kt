/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag.log

import nl.info.zac.log.LogEvent
import java.util.UUID

class ProductaanvraagObjectEvent(
    val type: Type,
    val productaanvraagObjectUuid: UUID
) : LogEvent(type.description) {
    enum class Type(val description: String) {
        PRODUCTAANVRAAG_DIMPACT_HANDLING_STARTED("Handling of a productaanvraag-Dimpact object started"),
        PRODUCTAANVRAAG_DIMPACT_HANDLING_FAILED("Failed to handle productaanvraag-Dimpact object"),
        PRODUCTAANVRAAG_OBJECT_UNREADABLE("Unable to read the productaanvraag object")
    }
}
