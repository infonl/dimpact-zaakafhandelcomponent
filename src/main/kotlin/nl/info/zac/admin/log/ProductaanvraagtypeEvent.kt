/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin.log

import nl.info.zac.log.LogEvent
import java.util.UUID

sealed class ProductaanvraagtypeEvent(
    description: String,
    val productaanvraagType: String,
    val zaaktypeOmschrijving: String,
    val zaaktypeUuid: UUID
) : LogEvent(description) {
    class MultipleZaaktypeConfigurationsFound(
        productaanvraagType: String,
        zaaktypeOmschrijving: String,
        zaaktypeUuid: UUID
    ) : ProductaanvraagtypeEvent(
        "Multiple zaaktype configurations found for the productaanvraag type; using the most recently created one",
        productaanvraagType,
        zaaktypeOmschrijving,
        zaaktypeUuid
    )
}
