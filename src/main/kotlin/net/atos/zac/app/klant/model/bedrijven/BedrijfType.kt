/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant.model.bedrijven

enum class BedrijfType(val type: String) {
    HOOFDVESTIGING("hoofdvestiging"),
    NEVENVESTIGING("nevenvestiging"),
    RECHTSPERSOON("rechtspersoon");

    companion object {
        fun getType(type: String?): BedrijfType? {
            if (type == null) {
                return null
            }
            for (bedrijfType in entries) {
                if (bedrijfType.type == type) {
                    return bedrijfType
                }
            }
            error("BedrijfType: '$type' not found")
        }
    }
}
