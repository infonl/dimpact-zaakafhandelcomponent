/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

import java.util.UUID

class ZaaktypeEvent(
    val type: Type,
    val zaaktypeUuid: UUID,
    val zaaktypeOmschrijving: String? = null
) : LogEvent(type.description) {
    enum class Type(val description: String) {
        TEMPLATE_MAPPING_STORING("Storing template mapping for zaaktype"),
        ZAAKTYPE_CONFIGURATION_UPDATING("Zaaktype configuration already exists; updating it"),
        CONCEPT_ZAAKTYPE_IGNORED("Zaaktype is still a concept; ignoring it"),
        ZAAKTYPE_WITHOUT_CONFIGURATION_IGNORED("Zaaktype has no known configuration; ignoring it")
    }
}
