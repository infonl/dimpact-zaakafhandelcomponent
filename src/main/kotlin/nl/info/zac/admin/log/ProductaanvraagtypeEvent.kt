/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin.log

import nl.info.zac.log.LogEvent
import java.util.UUID

class ProductaanvraagtypeEvent(
    val type: Type,
    val productaanvraagType: String,
    val zaaktypeOmschrijving: String,
    val zaaktypeUuid: UUID
) : LogEvent(type.description) {
    enum class Type(val description: String) {
        MULTIPLE_ZAAKTYPE_CONFIGURATIONS_FOUND(
            "Multiple zaaktype configurations found for the productaanvraag type; using the most recently created one"
        ),
        PRODUCTAANVRAAGTYPE_ALREADY_IN_USE("Productaanvraagtype is already in use by another zaaktype")
    }
}
