/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.shared

import jakarta.ws.rs.BadRequestException
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType
import org.eclipse.microprofile.openapi.annotations.media.Schema

@Schema(type = SchemaType.STRING, enumeration = ["asc", "desc"])
enum class RestSorteerRichting(val value: String) {
    ASC("asc"),
    DESC("desc");

    companion object {
        @JvmStatic
        fun fromString(value: String) =
            entries.firstOrNull { it.value == value }
                ?: throw BadRequestException("Unknown sort order '$value'")
    }
}
