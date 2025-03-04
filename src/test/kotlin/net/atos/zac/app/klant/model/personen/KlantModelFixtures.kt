/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant.model.personen

import net.atos.zac.app.klant.model.bedrijven.BedrijfType
import net.atos.zac.app.klant.model.bedrijven.RestListBedrijvenParameters

@Suppress("LongParameterList")
fun createRestListBedrijvenParameters(
    kvkNummer: String = "123456789",
    vestigingsnummer: String? = "dummyVestigingsnummer",
    rsin: String? = "dummyRsin",
    naam: String? = "dummyNaam",
    postcode: String? = "dummyPostcode",
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
