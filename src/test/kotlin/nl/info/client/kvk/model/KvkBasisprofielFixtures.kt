/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.kvk.model

import nl.info.client.kvk.basisprofiel.model.generated.Adres
import nl.info.client.kvk.basisprofiel.model.generated.Basisprofiel
import nl.info.client.kvk.basisprofiel.model.generated.Eigenaar
import nl.info.client.kvk.basisprofiel.model.generated.EmbeddedContainer
import nl.info.client.kvk.basisprofiel.model.generated.Handelsnaam
import nl.info.client.kvk.basisprofiel.model.generated.SBIActiviteit as BasisprofielSBIActiviteit

fun createHandelsnaam(
    naam: String = "fakeHandelsnaam",
    volgorde: Int? = 1
) = Handelsnaam().apply {
    this.naam = naam
    this.volgorde = volgorde
}

fun createBasisprofielSBIActiviteit(
    sbiOmschrijving: String = "fakeSbiOmschrijving",
    indHoofdactiviteit: String = "ja"
) = BasisprofielSBIActiviteit().apply {
    this.sbiOmschrijving = sbiOmschrijving
    this.indHoofdactiviteit = indHoofdactiviteit
}

fun createBasisprofielAdres(
    type: String = "fakeType",
    indAfgeschermd: String = "nee",
    volledigAdres: String = "fakeVolledigAdres"
) = Adres().apply {
    this.type = type
    this.indAfgeschermd = indAfgeschermd
    this.volledigAdres = volledigAdres
}

fun createEigenaar(
    rsin: String? = "fakeRsin",
    rechtsvorm: String? = "fakeRechtsvorm",
    uitgebreideRechtsvorm: String? = "fakeUitgebreideRechtsvorm",
    adressen: List<Adres>? = listOf(createBasisprofielAdres()),
    websites: List<String>? = listOf("https://fake.nl")
) = Eigenaar().apply {
    this.rsin = rsin
    this.rechtsvorm = rechtsvorm
    this.uitgebreideRechtsvorm = uitgebreideRechtsvorm
    this.adressen = adressen
    this.websites = websites
}

@Suppress("LongParameterList")
fun createBasisprofiel(
    kvkNummer: String = "fakeKvkNummer",
    totaalWerkzamePersonen: Int? = 10,
    statutaireNaam: String? = "fakeStatutaireNaam",
    handelsnamen: List<Handelsnaam>? = listOf(createHandelsnaam()),
    sbiActiviteiten: List<BasisprofielSBIActiviteit>? = listOf(
        createBasisprofielSBIActiviteit(sbiOmschrijving = "fakeHoofdactiviteit", indHoofdactiviteit = "ja"),
        createBasisprofielSBIActiviteit(sbiOmschrijving = "fakeNevenactiviteit", indHoofdactiviteit = "nee")
    ),
    eigenaar: Eigenaar? = createEigenaar()
) = Basisprofiel().apply {
    this.kvkNummer = kvkNummer
    this.totaalWerkzamePersonen = totaalWerkzamePersonen
    this.statutaireNaam = statutaireNaam
    this.handelsnamen = handelsnamen
    this.sbiActiviteiten = sbiActiviteiten
    this.embedded = if (eigenaar != null) EmbeddedContainer().apply { this.eigenaar = eigenaar } else null
}
