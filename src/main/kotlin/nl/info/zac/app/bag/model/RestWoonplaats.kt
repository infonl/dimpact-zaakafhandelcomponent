/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.bag.model

import nl.info.client.bag.model.generated.Indicatie
import nl.info.client.bag.model.generated.StatusWoonplaats
import nl.info.client.bag.model.generated.Woonplaats
import nl.info.client.bag.model.generated.WoonplaatsIOHal
import nl.info.client.bag.model.generated.WoonplaatsIOHalBasis
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.model.zaakobjecten.ObjectWoonplaats
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectWoonplaats
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectWoonplaatsRequest
import java.net.URI

class RestWoonplaats : RestBagObject() {
    var naam: String? = null

    var status: StatusWoonplaats? = null

    override val bagObjectType
        get() = BagObjectType.WOONPLAATS

    override val omschrijving
        get() = naam
}

fun WoonplaatsIOHalBasis.toRestWoonplaats() = woonplaats.toRestWoonplaats().apply {
    url = URI.create(this@toRestWoonplaats.links.self.href)
}

fun WoonplaatsIOHal.toRestWoonplaats() = woonplaats.toRestWoonplaats().apply {
    url = URI.create(this@toRestWoonplaats.links.self.href)
}

fun ZaakobjectWoonplaats.toRestWoonplaats() = objectIdentificatie?.let { objectWoonplaats ->
    RestWoonplaats().apply {
        url = this@toRestWoonplaats.`object`
        identificatie = objectWoonplaats.identificatie
        naam = objectWoonplaats.woonplaatsNaam
    }
}

fun RestWoonplaats.toZaakobjectWoonplaatsRequest(zaak: Zaak) =
    ZaakobjectWoonplaatsRequest(zaak.url, url, ObjectWoonplaats(identificatie, naam))

private fun Woonplaats.toRestWoonplaats() = RestWoonplaats().apply {
    identificatie = this@toRestWoonplaats.identificatie
    naam = this@toRestWoonplaats.naam
    status = this@toRestWoonplaats.status
    isGeconstateerd = this@toRestWoonplaats.geconstateerd == Indicatie.J
}
