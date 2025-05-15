/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.klant.model.bedrijven

enum class BedrijfType(val type: String) {
    HOOFDVESTIGING("hoofdvestiging"),
    NEVENVESTIGING("nevenvestiging"),
    RECHTSPERSOON("rechtspersoon")
}
