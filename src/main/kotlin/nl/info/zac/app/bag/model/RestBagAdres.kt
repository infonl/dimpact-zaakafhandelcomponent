/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.bag.model

import nl.info.client.bag.model.generated.AdresIOHal
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.model.zaakobjecten.ObjectAdres
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectAdres
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectAdresRequest
import nl.info.zac.app.zaak.model.RestGeometry
import nl.info.zac.app.zaak.model.RestGeometryType
import java.net.URI

class RestBagAdres : RestBagObject() {
    var postcode: String? = null

    var huisnummerWeergave: String? = null

    var huisnummer: Int = 0

    var huisletter: String? = null

    var huisnummertoevoeging: String? = null

    var openbareRuimteNaam: String? = null

    var woonplaatsNaam: String? = null

    var openbareRuimte: RestOpenbareRuimte? = null

    var nummeraanduiding: RestNummeraanduiding? = null

    var woonplaats: RestWoonplaats? = null

    var adresseerbaarObject: RestAdresseerbaarObject? = null

    var panden: List<RestPand> = emptyList()

    override val bagObjectType
        get() = BagObjectType.ADRES

    override val omschrijving
        get() = "$openbareRuimteNaam $huisnummerWeergave, $postcode $woonplaatsNaam"

    val geometry: RestGeometry?
        get() {
            val restGeometries = listOfNotNull(adresseerbaarObject?.geometry, panden.firstOrNull()?.geometry)
            return when (restGeometries.size) {
                1 -> restGeometries.first()
                2 -> RestGeometry(type = RestGeometryType.GEOMETRY_COLLECTION, geometrycollection = restGeometries)
                else -> null
            }
        }
}

fun AdresIOHal.toRestBagAdres() = RestBagAdres().apply {
    url = URI.create(this@toRestBagAdres.links.self.href)
    identificatie = this@toRestBagAdres.nummeraanduidingIdentificatie
    postcode = this@toRestBagAdres.postcode
    huisnummer = this@toRestBagAdres.huisnummer
    huisletter = this@toRestBagAdres.huisletter
    huisnummertoevoeging = this@toRestBagAdres.huisnummertoevoeging
    huisnummerWeergave = createHuisnummerWeergave(
        this@toRestBagAdres.huisnummer,
        this@toRestBagAdres.huisletter,
        this@toRestBagAdres.huisnummertoevoeging
    )
    openbareRuimteNaam = this@toRestBagAdres.openbareRuimteNaam
    woonplaatsNaam = this@toRestBagAdres.woonplaatsNaam
    isGeconstateerd = this@toRestBagAdres.geconstateerd?.let {
        it.nummeraanduiding == true && it.woonplaats == true && it.openbareRuimte == true
    } == true
    this@toRestBagAdres.embedded?.let { adresIOEmbedded ->
        openbareRuimte = adresIOEmbedded.openbareRuimte?.toRestOpenbareRuimte(this@toRestBagAdres)
        nummeraanduiding = adresIOEmbedded.nummeraanduiding?.toRestNummeraanduiding()
        woonplaats = adresIOEmbedded.woonplaats?.toRestWoonplaats()
        panden = adresIOEmbedded.panden.orEmpty().map { it.toRestPand() }
        adresseerbaarObject = adresIOEmbedded.adresseerbaarObject?.toRestAdresseerbaarObject()
    }
}

fun ZaakobjectAdres.toRestBagAdres() = objectIdentificatie?.let { objectAdres ->
    RestBagAdres().apply {
        url = this@toRestBagAdres.`object`
        identificatie = objectAdres.identificatie
        postcode = objectAdres.postcode
        huisnummerWeergave = createHuisnummerWeergave(
            objectAdres.huisnummer,
            objectAdres.huisletter,
            objectAdres.huisnummertoevoeging
        )
        openbareRuimteNaam = objectAdres.gorOpenbareRuimteNaam
        woonplaatsNaam = objectAdres.wplWoonplaatsNaam
    }
}

fun RestBagAdres.toZaakobjectAdresRequest(zaak: Zaak) =
    ZaakobjectAdresRequest(
        zaak.url,
        url,
        ObjectAdres(
            identificatie = identificatie,
            wplWoonplaatsNaam = woonplaatsNaam,
            gorOpenbareRuimteNaam = openbareRuimteNaam,
            huisnummer = huisnummer,
            huisletter = huisletter,
            huisnummertoevoeging = huisnummertoevoeging,
            postcode = postcode
        )
    )
