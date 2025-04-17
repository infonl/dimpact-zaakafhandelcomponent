/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.bag.model

import nl.info.client.bag.model.generated.AdresIOHal
import nl.info.client.bag.model.generated.AdresIOLinks
import nl.info.client.bag.model.generated.HalLink

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

fun createAdresIOLinks(
    self: HalLink = createHalLink()
) = AdresIOLinks().apply {
    this.self = self
}

fun createHalLink(
    href: String = "fakeHref"
) = HalLink().apply {
    this.href = href
}
