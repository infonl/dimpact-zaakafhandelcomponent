/*
 * SPDX-FileCopyrightText: 2023, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model.zaakobjecten

import nl.info.zac.util.NoArgConstructor

/**
 * ObjectNummeraanduiding
 */
@NoArgConstructor
data class ObjectNummeraanduiding(
    override var identificatie: String? = null,
    var huisnummer: Int = 0,
    var huisletter: String? = null,
    var huisnummertoevoeging: String? = null,
    var postcode: String? = null,
    var typeAdresseerbaarObject: String? = null,
    var status: String? = null
) : ObjectBagObject(identificatie) {
    var huisnummerWeergave: String? = null
}
