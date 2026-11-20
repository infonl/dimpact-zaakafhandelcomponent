/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import jakarta.ws.rs.BadRequestException

enum class RestInrichtingscheckSortField {
    OMSCHRIJVING,
    DOEL,
    BEGIN_GELDIGHEID,
    VALIDE;

    companion object {
        @JvmStatic
        fun fromString(value: String) =
            entries.firstOrNull { it.name == value }
                ?: throw BadRequestException("Unknown sort field '$value'")
    }
}
