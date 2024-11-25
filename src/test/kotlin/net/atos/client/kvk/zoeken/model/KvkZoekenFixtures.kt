/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.kvk.zoeken.model

import net.atos.client.kvk.zoeken.model.generated.Adres
import net.atos.client.kvk.zoeken.model.generated.AdresType
import net.atos.client.kvk.zoeken.model.generated.BinnenlandsAdres
import net.atos.client.kvk.zoeken.model.generated.ResultaatItem

@Suppress("LongParameterList")
fun createAdresWithBinnenlandsAdres(
    huisnummer: Int = 1234,
    huisletter: String = "dummyHuisletter",
    plaats: String = "dummyPlaats",
    postbusnummer: Int = 5678,
    postcode: String = "dummyPostcode",
    straatnaam: String = "dummyStraatnaam",
    type: AdresType = AdresType.BEZOEKADRES
) = Adres().apply {
    this.binnenlandsAdres = BinnenlandsAdres().apply {
        this.huisnummer = huisnummer
        this.huisletter = huisletter
        this.plaats = plaats
        this.postbusnummer = postbusnummer
        this.postcode = postcode
        this.straatnaam = straatnaam
        this.type = type
    }
}

fun createResultaatItem(
    adres: Adres = createAdresWithBinnenlandsAdres(),
    naam: String = "dummyNaam",
    kvkNummer: String = "dummyKvkNummer",
    type: String = "nevenvestiging",
    vestingsnummer: String = "dummyVestigingsnummer",
) = ResultaatItem().apply {
    this.adres = adres
    this.naam = naam
    this.type = type
    this.kvkNummer = kvkNummer
    this.vestigingsnummer = vestingsnummer
}
