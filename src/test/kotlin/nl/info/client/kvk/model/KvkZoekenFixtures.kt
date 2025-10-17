/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.kvk.model

import nl.info.client.kvk.vestigingsprofiel.model.generated.SBIActiviteit
import nl.info.client.kvk.vestigingsprofiel.model.generated.Vestiging
import nl.info.client.kvk.zoeken.model.generated.Adres
import nl.info.client.kvk.zoeken.model.generated.AdresType
import nl.info.client.kvk.zoeken.model.generated.BinnenlandsAdres
import nl.info.client.kvk.zoeken.model.generated.ResultaatItem
import kotlin.random.Random

@Suppress("LongParameterList")
fun createAdresWithBinnenlandsAdres(
    huisnummer: Int = 1234,
    huisletter: String = "fakeHuisletter",
    plaats: String = "fakePlaats",
    postbusnummer: Int = 5678,
    postcode: String = "fakePostcode",
    straatnaam: String = "fakeStraatnaam",
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

@Suppress("LongParameterList")
fun createResultaatItem(
    adres: Adres = createAdresWithBinnenlandsAdres(),
    naam: String = "fakeNaam",
    kvkNummer: String? = "fakeKvkNummer",
    rsin: String? = "fakeRsin",
    type: String = "nevenvestiging",
    vestingsnummer: String? = "fakeVestigingsnummer",
) = ResultaatItem().apply {
    this.adres = adres
    this.naam = naam
    this.type = type
    this.kvkNummer = kvkNummer
    this.rsin = rsin
    this.vestigingsnummer = vestingsnummer
}

fun createSBIActiviteit(
    sbiCode: String = "fakeSbiCode",
    sbiOmschrijving: String = "fakeSbiOmschrijving",
    indHoofdactiviteit: String = "ja",
) = SBIActiviteit().apply {
    this.sbiCode = sbiCode
    this.sbiOmschrijving = sbiOmschrijving
    this.indHoofdactiviteit = indHoofdactiviteit
}

@Suppress("LongParameterList")
fun createVestiging(
    vestigingsnummer: String = "fakeVestigingsnummer",
    kvkNumber: String = "fakeKvkNummer",
    eersteHandelsNaam: String = "fakeEersteHandelsNaam",
    voltijdWerkzamePersonen: Int? = 10,
    deeltijdWerkzamePersonen: Int? = 5,
    totaalWerkzamePersonen: Int? = 15,
    sbiActiviteiten: List<SBIActiviteit>? = listOf(
        createSBIActiviteit(
            sbiCode = "fakeSbiCode1",
            sbiOmschrijving = "fakeSbiOmschrijving1",
            indHoofdactiviteit = "ja"
        ),
        createSBIActiviteit(
            sbiCode = "fakeSbiCode2",
            sbiOmschrijving = "fakeSbiOmschrijving2",
            indHoofdactiviteit = "nee"
        )
    ),
    adressen: List<nl.info.client.kvk.vestigingsprofiel.model.generated.Adres>? = listOf(createVestigingsAdres())
) = Vestiging().apply {
    this.vestigingsnummer = vestigingsnummer
    this.kvkNummer = kvkNumber
    this.eersteHandelsnaam = eersteHandelsNaam
    this.voltijdWerkzamePersonen = voltijdWerkzamePersonen
    this.deeltijdWerkzamePersonen = deeltijdWerkzamePersonen
    this.totaalWerkzamePersonen = totaalWerkzamePersonen
    this.sbiActiviteiten = sbiActiviteiten
    this.adressen = adressen
}

fun createVestigingsAdres(
    type: String = "fakeType",
    indAfgeschermd: String = "nee",
    volledigAdres: String = "fakeVolledigAdres"
) = nl.info.client.kvk.vestigingsprofiel.model.generated.Adres().apply {
    this.type = type
    this.indAfgeschermd = indAfgeschermd
    this.volledigAdres = volledigAdres
}

fun createRandomVestigingsNumber() = createRandomDigitsString(12)

fun createRandomKvkNumber() = createRandomDigitsString(8)

fun createRandomDigitsString(length: Int): String {
    return (1..length).map { Random.nextInt(0, 10) }.joinToString("")
}
