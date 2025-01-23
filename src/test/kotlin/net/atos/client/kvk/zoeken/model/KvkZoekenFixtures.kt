/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.kvk.zoeken.model

import net.atos.client.kvk.vestigingsprofiel.model.generated.SBIActiviteit
import net.atos.client.kvk.vestigingsprofiel.model.generated.Vestiging
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

fun createSBIActiviteit(
    sbiCode: String = "dummySbiCode",
    sbiOmschrijving: String = "dummySbiOmschrijving",
    indHoofdactiviteit: String = "ja",
) = SBIActiviteit().apply {
    this.sbiCode = sbiCode
    this.sbiOmschrijving = sbiOmschrijving
    this.indHoofdactiviteit = indHoofdactiviteit
}

@Suppress("LongParameterList")
fun createVestiging(
    vestigingsnummer: String = "dummyVestigingsnummer",
    kvkNumber: String = "dummyKvkNummer",
    eersteHandelsNaam: String = "dummyEersteHandelsNaam",
    voltijdWerkzamePersonen: Int? = 10,
    deeltijdWerkzamePersonen: Int? = 5,
    totaalWerkzamePersonen: Int? = 15,
    sbiActiviteiten: List<SBIActiviteit>? = listOf(
        createSBIActiviteit(
            sbiCode = "dummySbiCode1",
            sbiOmschrijving = "dummySbiOmschrijving1",
            indHoofdactiviteit = "ja"
        ),
        createSBIActiviteit(
            sbiCode = "dummySbiCode2",
            sbiOmschrijving = "dummySbiOmschrijving2",
            indHoofdactiviteit = "nee"
        )
    ),
) = Vestiging().apply {
    this.vestigingsnummer = vestigingsnummer
    this.kvkNummer = kvkNumber
    this.eersteHandelsnaam = eersteHandelsNaam
    this.voltijdWerkzamePersonen = voltijdWerkzamePersonen
    this.deeltijdWerkzamePersonen = deeltijdWerkzamePersonen
    this.totaalWerkzamePersonen = totaalWerkzamePersonen
    this.sbiActiviteiten = sbiActiviteiten
}
