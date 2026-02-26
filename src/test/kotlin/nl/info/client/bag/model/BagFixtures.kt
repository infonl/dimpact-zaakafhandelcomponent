/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.bag.model

import nl.info.client.bag.model.generated.AdresIOHal
import nl.info.client.bag.model.generated.AdresIOHalCollectionEmbedded
import nl.info.client.bag.model.generated.AdresIOLinks
import nl.info.client.bag.model.generated.HalLink
import nl.info.client.bag.model.generated.Nummeraanduiding
import nl.info.client.bag.model.generated.NummeraanduidingIOHal
import nl.info.client.bag.model.generated.OpenbareRuimte
import nl.info.client.bag.model.generated.OpenbareRuimteIOHal
import nl.info.client.bag.model.generated.Pand
import nl.info.client.bag.model.generated.PandIOHal
import nl.info.client.bag.model.generated.Woonplaats
import nl.info.client.bag.model.generated.WoonplaatsIOHal
import nl.info.client.bag.model.generated.WoonplaatsIOHalBasis

@Suppress("LongParameterList")
fun createAdresIOHal(
    huisnummer: Int = 123,
    huisletter: String = "fakeHuisletter",
    huisnummertoevoeging: String = "fakeHuisnummertoevoeging",
    postcode: String = "fakePostcode",
    woonplaatsNaam: String = "fakeWoonplaatsNaam",
    links: AdresIOLinks = createAdresIOLinks()
) = AdresIOHal().apply {
    this.huisnummer = huisnummer
    this.huisletter = huisletter
    this.huisnummertoevoeging = huisnummertoevoeging
    this.postcode = postcode
    this.woonplaatsNaam = woonplaatsNaam
    this.links = links
}

fun createAdresIOHalCollectionEmbedded(
    addressen: List<AdresIOHal>? = listOf(createAdresIOHal())
) = AdresIOHalCollectionEmbedded().apply {
    this.adressen = addressen
}

fun createAdresIOLinks(
    self: HalLink = createHalLink()
) = AdresIOLinks().apply {
    this.self = self
}

fun createBevraagAdressenParameters(
    zoekresultaatIdentificatie: String = "fakeZoekresultaatIdentificatie"
) = BevraagAdressenParameters().apply {
    this.zoekresultaatIdentificatie = zoekresultaatIdentificatie
}

fun createHalLink(
    href: String = "fakeHref"
) = HalLink().apply {
    this.href = href
}

fun createWoonplaats(
    name: String = "fakeWoonplaatsName"
) = Woonplaats().apply {
    this.naam = name
}

fun createWoonplaatsIOHal(
    woonplaats: Woonplaats = createWoonplaats()
) = WoonplaatsIOHal().apply {
    this.woonplaats = woonplaats
}

fun createNummeraanduiding(
    huisnummer: Int = 123
) = Nummeraanduiding().apply {
    this.huisnummer = huisnummer
}

fun createNummeraanduidingIOHal(
    nummeraanduiding: Nummeraanduiding = createNummeraanduiding()
) = NummeraanduidingIOHal().apply {
    this.nummeraanduiding = nummeraanduiding
}

fun createPand(
    oorspronkelijkBouwjaar: String = "2000"
) = Pand().apply {
    this.oorspronkelijkBouwjaar = oorspronkelijkBouwjaar
}

fun createPandIOHal(
    pand: Pand = createPand()
) = PandIOHal().apply {
    this.pand = pand
}

fun createOpenbareRuimte(
    naam: String = "fakeOpenbareRuimteNaam"
) = OpenbareRuimte().apply {
    this.naam = naam
}

fun createOpenbareRuimteIOHal(
    openbareRuimte: OpenbareRuimte = createOpenbareRuimte()
) = OpenbareRuimteIOHal().apply {
    this.openbareRuimte = openbareRuimte
}

fun createWoonplaatsIOHalBasis(
    woonplaats: Woonplaats = createWoonplaats()
) = WoonplaatsIOHalBasis().apply {
    this.woonplaats = woonplaats
}
