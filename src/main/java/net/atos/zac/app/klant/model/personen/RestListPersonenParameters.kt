/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant.model.personen

import java.time.LocalDate

data class RestListPersonenParameters(
    var bsn: String? = null,
    var geslachtsnaam: String? = null,
    var voornamen: String? = null,
    var voorvoegsel: String? = null,
    var geboortedatum: LocalDate? = null,
    var gemeenteVanInschrijving: String? = null,
    var postcode: String? = null,
    var huisnummer: Int? = null,
    var straat: String? = null
)
