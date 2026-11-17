/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import jakarta.ws.rs.BadRequestException

enum class RestInrichtingscheckStatus {
    AANDACHT_NODIG,
    IN_ORDE;

    companion object {
        @JvmStatic
        fun fromString(value: String) =
            entries.firstOrNull { it.name == value }
                ?: throw BadRequestException("Unknown status '$value'")
    }
}
