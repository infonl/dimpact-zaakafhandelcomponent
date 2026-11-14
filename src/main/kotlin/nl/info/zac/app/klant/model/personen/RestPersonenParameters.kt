/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.klant.model.personen

import nl.info.zac.app.klant.model.personen.RestPersonenParameters.Cardinaliteit

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

/**
 * Needs to correspond to the implementation [toPersonenQuery] function.
 */
val VALID_PERSONEN_QUERIES = listOf(
    RestPersonenParameters(
        bsn = Cardinaliteit.REQ,
        geslachtsnaam = Cardinaliteit.NON,
        voornamen = Cardinaliteit.NON,
        voorvoegsel = Cardinaliteit.NON,
        geboortedatum = Cardinaliteit.NON,
        gemeenteVanInschrijving = Cardinaliteit.NON,
        postcode = Cardinaliteit.NON,
        huisnummer = Cardinaliteit.NON,
        straat = Cardinaliteit.NON
    ),
    RestPersonenParameters(
        bsn = Cardinaliteit.NON,
        geslachtsnaam = Cardinaliteit.REQ,
        voornamen = Cardinaliteit.OPT,
        voorvoegsel = Cardinaliteit.OPT,
        geboortedatum = Cardinaliteit.REQ,
        gemeenteVanInschrijving = Cardinaliteit.NON,
        postcode = Cardinaliteit.NON,
        huisnummer = Cardinaliteit.NON,
        straat = Cardinaliteit.NON
    ),
    RestPersonenParameters(
        bsn = Cardinaliteit.NON,
        geslachtsnaam = Cardinaliteit.REQ,
        voornamen = Cardinaliteit.REQ,
        voorvoegsel = Cardinaliteit.OPT,
        geboortedatum = Cardinaliteit.NON,
        gemeenteVanInschrijving = Cardinaliteit.REQ,
        postcode = Cardinaliteit.NON,
        huisnummer = Cardinaliteit.NON,
        straat = Cardinaliteit.NON
    ),
    RestPersonenParameters(
        bsn = Cardinaliteit.NON,
        geslachtsnaam = Cardinaliteit.NON,
        voornamen = Cardinaliteit.NON,
        voorvoegsel = Cardinaliteit.NON,
        geboortedatum = Cardinaliteit.NON,
        gemeenteVanInschrijving = Cardinaliteit.NON,
        postcode = Cardinaliteit.REQ,
        huisnummer = Cardinaliteit.REQ,
        straat = Cardinaliteit.NON
    ),
    RestPersonenParameters(
        bsn = Cardinaliteit.NON,
        geslachtsnaam = Cardinaliteit.NON,
        voornamen = Cardinaliteit.NON,
        voorvoegsel = Cardinaliteit.NON,
        geboortedatum = Cardinaliteit.NON,
        gemeenteVanInschrijving = Cardinaliteit.REQ,
        postcode = Cardinaliteit.NON,
        huisnummer = Cardinaliteit.REQ,
        straat = Cardinaliteit.REQ
    )
)
