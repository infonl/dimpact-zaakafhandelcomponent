/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant.model.bedrijven

import net.atos.client.kvk.model.KvkZoekenParameters

data class RestListBedrijvenParameters(
    var kvkNummer: String? = null,
    var vestigingsnummer: String? = null,
    var rsin: String? = null,
    var naam: String? = null,
    var postcode: String? = null,
    var huisnummer: Int? = null,
    var type: BedrijfType? = null,
)

fun RestListBedrijvenParameters.toKvkZoekenParameters() = KvkZoekenParameters().apply {
    kvkNummer = this@toKvkZoekenParameters.kvkNummer
    vestigingsnummer = this@toKvkZoekenParameters.vestigingsnummer
    rsin = this@toKvkZoekenParameters.rsin
    naam = this@toKvkZoekenParameters.naam
    type = this@toKvkZoekenParameters.type?.type
    postcode = this@toKvkZoekenParameters.postcode
    huisnummer = this@toKvkZoekenParameters.huisnummer?.toString()
}
