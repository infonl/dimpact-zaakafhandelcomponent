/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.bag.model

import nl.info.client.bag.model.generated.AdresIOHal
import nl.info.client.bag.model.generated.Indicatie
import nl.info.client.bag.model.generated.OpenbareRuimte
import nl.info.client.bag.model.generated.OpenbareRuimteIOHal
import nl.info.client.bag.model.generated.OpenbareRuimteIOHalBasis
import nl.info.client.bag.model.generated.StatusNaamgeving
import nl.info.client.bag.model.generated.TypeOpenbareRuimte
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.model.zaakobjecten.ObjectOpenbareRuimte
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectOpenbareRuimte
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectOpenbareRuimteRequest
import java.net.URI

class RestOpenbareRuimte : RestBagObject() {
    var naam: String? = null

    var type: TypeOpenbareRuimte? = null

    var typeWeergave: String? = null

    var status: StatusNaamgeving? = null

    var woonplaatsNaam: String? = null

    var woonplaats: RestWoonplaats? = null

    override val bagObjectType
        get() = BagObjectType.OPENBARE_RUIMTE

    override val omschrijving
        get() = naam
}

fun OpenbareRuimteIOHalBasis.toRestOpenbareRuimte(adres: AdresIOHal) = toRestOpenbareRuimte().apply {
    woonplaatsNaam = adres.woonplaatsNaam
}

fun OpenbareRuimteIOHalBasis.toRestOpenbareRuimte() = openbareRuimte.toRestOpenbareRuimte().apply {
    url = URI.create(this@toRestOpenbareRuimte.links.self.href)
}

fun OpenbareRuimteIOHal.toRestOpenbareRuimte() = openbareRuimte.toRestOpenbareRuimte().apply {
    url = URI.create(this@toRestOpenbareRuimte.links.self.href)
    woonplaats = this@toRestOpenbareRuimte.embedded?.ligtInWoonplaats?.toRestWoonplaats()
}

fun ZaakobjectOpenbareRuimte.toRestOpenbareRuimte() = objectIdentificatie?.let { objectOpenbareRuimte ->
    RestOpenbareRuimte().apply {
        url = this@toRestOpenbareRuimte.`object`
        identificatie = objectOpenbareRuimte.identificatie
        naam = objectOpenbareRuimte.gorOpenbareRuimteNaam
        woonplaatsNaam = objectOpenbareRuimte.wplWoonplaatsNaam
    }
}

fun RestOpenbareRuimte.toZaakobjectOpenbareRuimteRequest(zaak: Zaak) =
    ZaakobjectOpenbareRuimteRequest(
        zaak.url,
        url,
        ObjectOpenbareRuimte(identificatie, naam, woonplaatsNaam)
    )

private fun OpenbareRuimte.toRestOpenbareRuimte() = RestOpenbareRuimte().apply {
    identificatie = this@toRestOpenbareRuimte.identificatie
    naam = this@toRestOpenbareRuimte.naam
    woonplaatsNaam = this@toRestOpenbareRuimte.ligtIn
    status = this@toRestOpenbareRuimte.status
    type = this@toRestOpenbareRuimte.type
    typeWeergave = this@toRestOpenbareRuimte.type?.toString()
    isGeconstateerd = this@toRestOpenbareRuimte.geconstateerd == Indicatie.J
}
