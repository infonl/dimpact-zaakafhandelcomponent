/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.bag.model

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.model.zaakobjecten.Zaakobject
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectAdres
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectNummeraanduiding
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectOpenbareRuimte
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectPand
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectRequest
import nl.info.client.zgw.zrc.model.zaakobjecten.ZaakobjectWoonplaats
import org.apache.commons.lang3.NotImplementedException
import java.net.URI

sealed class RestBagObject {
    var url: URI? = null

    var identificatie: String? = null

    @get:JsonbProperty("geconstateerd")
    @set:JsonbProperty("geconstateerd")
    var isGeconstateerd: Boolean = false

    abstract val bagObjectType: BagObjectType

    abstract val omschrijving: String?
}

fun RestBagObject.toZaakobjectRequest(zaak: Zaak): ZaakobjectRequest =
    when (this) {
        is RestBagAdres -> toZaakobjectAdresRequest(zaak)
        is RestPand -> toZaakobjectPandRequest(zaak)
        is RestWoonplaats -> toZaakobjectWoonplaatsRequest(zaak)
        is RestOpenbareRuimte -> toZaakobjectOpenbareRuimteRequest(zaak)
        is RestNummeraanduiding -> toZaakobjectNummeraanduidingRequest(zaak)
        is RestAdresseerbaarObject -> throw NotImplementedException()
    }

fun Zaakobject.toRestBagObject(): RestBagObject? =
    when (this) {
        is ZaakobjectAdres -> toRestBagAdres()
        is ZaakobjectPand -> toRestPand()
        is ZaakobjectWoonplaats -> toRestWoonplaats()
        is ZaakobjectOpenbareRuimte -> toRestOpenbareRuimte()
        is ZaakobjectNummeraanduiding -> toRestNummeraanduiding()
        else -> error("Unexpected objectType: $objectType")
    }

fun createHuisnummerWeergave(huisnummer: Int, huisletter: String?, huisnummertoevoeging: String?) =
    buildString {
        append(huisnummer)
        if (!huisletter.isNullOrBlank()) {
            append(huisletter)
        }
        if (!huisnummertoevoeging.isNullOrBlank()) {
            append("-").append(huisnummertoevoeging)
        }
    }.trim()
