/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.bag.model

import nl.info.client.bag.model.generated.Indicatie
import nl.info.client.bag.model.generated.Pand
import nl.info.client.bag.model.generated.PandIOHal
import nl.info.client.bag.model.generated.PandIOHalBasis
import nl.info.client.bag.model.generated.StatusPand
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.model.zaakobjecten.ObjectPand
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectPand
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectPandRequest
import nl.info.zac.app.zaak.model.RestGeometry
import nl.info.zac.app.zaak.model.toRestGeometry
import java.net.URI

class RestPand : RestBagObject() {
    var oorspronkelijkBouwjaar: String? = null

    var status: StatusPand? = null

    var statusWeergave: String? = null

    var geometry: RestGeometry? = null

    override val bagObjectType
        get() = BagObjectType.PAND

    override val omschrijving
        get() = identificatie
}

fun PandIOHalBasis.toRestPand() = pand.toRestPand()

fun PandIOHal.toRestPand() = pand.toRestPand().apply {
    url = URI.create(this@toRestPand.links.self.href)
}

fun ZaakobjectPand.toRestPand() = objectIdentificatie?.let { objectPand ->
    RestPand().apply {
        identificatie = objectPand.identificatie
    }
}

fun RestPand.toZaakobjectPandRequest(zaak: Zaak) = ZaakobjectPandRequest(zaak.url, url, ObjectPand(identificatie))

private fun Pand.toRestPand() = RestPand().apply {
    identificatie = this@toRestPand.identificatie
    status = this@toRestPand.status
    statusWeergave = this@toRestPand.status?.toString()
    oorspronkelijkBouwjaar = this@toRestPand.oorspronkelijkBouwjaar
    isGeconstateerd = this@toRestPand.geconstateerd == Indicatie.J
    geometry = this@toRestPand.geometrie.toRestGeometry()
}
