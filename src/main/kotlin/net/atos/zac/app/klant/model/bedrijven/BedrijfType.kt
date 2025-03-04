/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant.model.bedrijven

enum class BedrijfType(val type: String) {
    HOOFDVESTIGING("hoofdvestiging"),
    NEVENVESTIGING("nevenvestiging"),
    RECHTSPERSOON("rechtspersoon")
}
