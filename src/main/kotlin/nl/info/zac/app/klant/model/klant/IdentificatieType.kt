/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.klant.model.klant

enum class IdentificatieType {
    BSN,
    VN,
    RSIN;

    fun isBsn(): Boolean = this == BSN
    fun isKvK(): Boolean = this == RSIN || this == VN
}
