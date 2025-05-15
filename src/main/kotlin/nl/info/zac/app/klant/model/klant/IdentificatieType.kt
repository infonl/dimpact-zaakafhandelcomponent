/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.klant.model.klant

enum class IdentificatieType(
    val isBsn: Boolean = false,
    val isKvK: Boolean = false
) {
    BSN(isBsn = true),
    VN(isKvK = true),
    RSIN(isKvK = true)
}
