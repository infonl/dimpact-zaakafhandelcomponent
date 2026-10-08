/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.bag.model

import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.model.zaakobjecten.Zaakobject
import java.util.UUID

class RestBagObjectGegevens {
    var uuid: UUID? = null

    lateinit var zaakUuid: UUID

    var zaakobject: RestBagObject? = null

    var redenWijzigen: String? = null

    val bagObject: RestBagObject?
        get() = zaakobject
}

fun Zaakobject.toRestBagObjectGegevens() = RestBagObjectGegevens().apply {
    zaakobject = this@toRestBagObjectGegevens.toRestBagObject()
    uuid = this@toRestBagObjectGegevens.uuid
    zaakUuid = this@toRestBagObjectGegevens.zaak.extractUuid()
}
