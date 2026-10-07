/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.bag.model

data class RestListAdressenParameters(
    var type: BagObjectType? = null,
    var trefwoorden: String? = null,
    var postcode: String? = null,
    var huisnummer: Int? = null
)
