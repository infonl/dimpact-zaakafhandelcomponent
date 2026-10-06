/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.klant.model.klant

interface RestKlant {
    var emailadres: String?
    var naam: String?
    var telefoonnummer: String?
    fun getIdentificatieType(): IdentificatieType?
}
