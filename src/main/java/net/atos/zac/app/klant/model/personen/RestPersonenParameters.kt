/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant.model.personen

data class RestPersonenParameters(
    var bsn: Cardinaliteit,
    var geslachtsnaam: Cardinaliteit,
    var voornamen: Cardinaliteit,
    var voorvoegsel: Cardinaliteit,
    var geboortedatum: Cardinaliteit,
    var gemeenteVanInschrijving: Cardinaliteit,
    var postcode: Cardinaliteit,
    var huisnummer: Cardinaliteit,
    var straat: Cardinaliteit
) {
    enum class Cardinaliteit {
        NON, // niet beschikbaar
        OPT, // optioneel
        REQ // verplicht
    }
}
