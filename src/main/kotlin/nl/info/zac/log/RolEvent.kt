/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.log

import java.util.UUID

class RolEvent(
    val type: Type,
    val rolUuid: UUID?,
    val betrokkeneType: String? = null
) : LogEvent(type.description) {
    enum class Type(val description: String) {
        INITIATOR_ROLE_WITHOUT_IDENTIFICATIE("Initiator role has no identificatie; cannot resolve initiator variables"),
        INITIATOR_WITHOUT_NAME("Initiator persoon does not have a name; using a replacement for the full name"),
        UNSUPPORTED_BETROKKENE_TYPE("Unsupported betrokkene type"),
        UNSUPPORTED_IDENTIFICATION_FIELDS("Unsupported identification fields for betrokkene type")
    }
}
