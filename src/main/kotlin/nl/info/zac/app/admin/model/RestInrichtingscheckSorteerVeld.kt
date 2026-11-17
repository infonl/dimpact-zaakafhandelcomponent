/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import jakarta.ws.rs.BadRequestException
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Schema

@Schema(type = SchemaType.STRING, enumeration = ["omschrijving", "doel", "beginGeldigheid", "valide"])
enum class RestInrichtingscheckSorteerVeld(val value: String) {
    OMSCHRIJVING("omschrijving"),
    DOEL("doel"),
    BEGIN_GELDIGHEID("beginGeldigheid"),
    VALIDE("valide");

    companion object {
        @JvmStatic
        fun fromString(value: String) =
            entries.firstOrNull { it.value == value }
                ?: throw BadRequestException("Unknown sort field '$value'")
    }
}
