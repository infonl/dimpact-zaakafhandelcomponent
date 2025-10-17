/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.klant.model.personen

import nl.info.client.kvk.model.BedrijfType
import nl.info.zac.app.klant.model.bedrijven.RestListBedrijvenParameters

@Suppress("LongParameterList")
fun createRestListBedrijvenParameters(
    kvkNummer: String? = "123456789",
    vestigingsnummer: String? = "fakeVestigingsnummer",
    rsin: String? = "fakeRsin",
    naam: String? = "fakeNaam",
    postcode: String? = "fakePostcode",
    huisnummer: Int? = 1234,
    type: BedrijfType? = BedrijfType.HOOFDVESTIGING,
) = RestListBedrijvenParameters(
    kvkNummer = kvkNummer,
    vestigingsnummer = vestigingsnummer,
    rsin = rsin,
    naam = naam,
    postcode = postcode,
    huisnummer = huisnummer,
    type = type
)
