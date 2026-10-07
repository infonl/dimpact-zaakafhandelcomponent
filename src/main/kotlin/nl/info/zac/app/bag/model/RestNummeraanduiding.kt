/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.bag.model

import nl.info.client.bag.model.generated.Indicatie
import nl.info.client.bag.model.generated.Nummeraanduiding
import nl.info.client.bag.model.generated.NummeraanduidingIOHal
import nl.info.client.bag.model.generated.NummeraanduidingIOHalBasis
import nl.info.client.bag.model.generated.StatusNaamgeving
import nl.info.client.bag.model.generated.TypeAdresseerbaarObject
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.model.zaakobjecten.ObjectNummeraanduiding
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectNummeraanduiding
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectNummeraanduidingRequest
import java.net.URI

class RestNummeraanduiding : RestBagObject() {
    var huisnummerWeergave: String? = null

    var huisnummer: Int = 0

    var huisletter: String? = null

    var huisnummertoevoeging: String? = null

    var postcode: String? = null

    var typeAdresseerbaarObject: TypeAdresseerbaarObject? = null

    var status: StatusNaamgeving? = null

    var woonplaats: RestWoonplaats? = null

    var openbareRuimte: RestOpenbareRuimte? = null

    override val bagObjectType
        get() = BagObjectType.NUMMERAANDUIDING

    override val omschrijving
        get() = "$huisnummerWeergave $postcode"
}

fun NummeraanduidingIOHalBasis.toRestNummeraanduiding() = nummeraanduiding.toRestNummeraanduiding().apply {
    url = URI.create(this@toRestNummeraanduiding.links.self.href)
}

fun NummeraanduidingIOHal.toRestNummeraanduiding() = nummeraanduiding.toRestNummeraanduiding().apply {
    url = URI.create(this@toRestNummeraanduiding.links.self.href)
    this@toRestNummeraanduiding.embedded?.let { nummeraanduidingEmbedded ->
        woonplaats = nummeraanduidingEmbedded.ligtInWoonplaats?.toRestWoonplaats()
        openbareRuimte = nummeraanduidingEmbedded.ligtAanOpenbareRuimte?.toRestOpenbareRuimte()
    }
}

fun ZaakobjectNummeraanduiding.toRestNummeraanduiding() =
    objectIdentificatie?.overigeData?.let { objectNummeraanduiding ->
        RestNummeraanduiding().apply {
            url = this@toRestNummeraanduiding.`object`
            identificatie = objectNummeraanduiding.identificatie
            postcode = objectNummeraanduiding.postcode
            huisnummer = objectNummeraanduiding.huisnummer
            huisletter = objectNummeraanduiding.huisletter
            huisnummertoevoeging = objectNummeraanduiding.huisnummertoevoeging
            huisnummerWeergave = createHuisnummerWeergave(
                objectNummeraanduiding.huisnummer,
                objectNummeraanduiding.huisletter,
                objectNummeraanduiding.huisnummertoevoeging
            )
            status = StatusNaamgeving.fromValue(objectNummeraanduiding.status)
            typeAdresseerbaarObject = TypeAdresseerbaarObject.fromValue(objectNummeraanduiding.typeAdresseerbaarObject)
        }
    }

fun RestNummeraanduiding.toZaakobjectNummeraanduidingRequest(zaak: Zaak) =
    ZaakobjectNummeraanduidingRequest(
        zaak.url,
        url,
        ObjectNummeraanduiding(
            identificatie = identificatie,
            huisnummer = huisnummer,
            huisletter = huisletter,
            huisnummertoevoeging = huisnummertoevoeging,
            postcode = postcode,
            typeAdresseerbaarObject = typeAdresseerbaarObject?.toString(),
            status = status?.toString()
        )
    )

fun Nummeraanduiding.toRestNummeraanduiding() = RestNummeraanduiding().apply {
    identificatie = this@toRestNummeraanduiding.identificatie
    postcode = this@toRestNummeraanduiding.postcode
    huisnummer = this@toRestNummeraanduiding.huisnummer
    huisletter = this@toRestNummeraanduiding.huisletter
    huisnummertoevoeging = this@toRestNummeraanduiding.huisnummertoevoeging
    huisnummerWeergave = createHuisnummerWeergave(
        this@toRestNummeraanduiding.huisnummer,
        this@toRestNummeraanduiding.huisletter,
        this@toRestNummeraanduiding.huisnummertoevoeging
    )
    status = this@toRestNummeraanduiding.status
    typeAdresseerbaarObject = this@toRestNummeraanduiding.typeAdresseerbaarObject
    isGeconstateerd = this@toRestNummeraanduiding.geconstateerd == Indicatie.J
}
