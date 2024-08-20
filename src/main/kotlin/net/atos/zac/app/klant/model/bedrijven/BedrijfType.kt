/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant.model.bedrijven

enum class BedrijfType(@JvmField val type: String) {
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
            throw IllegalStateException("BedrijfType: '$type' not found")
        }
    }
}
