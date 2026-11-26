/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.productaanvraag.log

import nl.info.zac.log.LogEvent
import java.util.UUID

sealed class ProductaanvraagObjectEvent(description: String, val productaanvraagObjectUuid: UUID) : LogEvent(description) {
    class ProductaanvraagDimpactHandlingFailed(productaanvraagObjectUuid: UUID) :
        ProductaanvraagObjectEvent("Failed to handle productaanvraag-Dimpact object", productaanvraagObjectUuid)

    class ProductaanvraagDimpactHandlingStarted(productaanvraagObjectUuid: UUID) :
        ProductaanvraagObjectEvent("Handling of a productaanvraag-Dimpact object started", productaanvraagObjectUuid)

    class ProductaanvraagObjectUnreadable(productaanvraagObjectUuid: UUID) :
        ProductaanvraagObjectEvent("Unable to read the productaanvraag object", productaanvraagObjectUuid)
}
