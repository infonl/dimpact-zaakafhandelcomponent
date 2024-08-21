/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant.model.bedrijven

import net.atos.client.kvk.model.KvkZoekenParameters
import org.apache.commons.lang3.StringUtils

data class RestListBedrijvenParameters(
    var kvkNummer: String? = null,
    var vestigingsnummer: String? = null,
    var rsin: String? = null,
    var naam: String? = null,
    var postcode: String? = null,
    var huisnummer: Int? = null,
    var type: BedrijfType? = null,
)

fun RestListBedrijvenParameters.toKvkZoekenParameters(): KvkZoekenParameters {
    val zoekenParameters = KvkZoekenParameters()
    if (StringUtils.isNotBlank(this.kvkNummer)) {
        zoekenParameters.kvkNummer = this.kvkNummer
    }
    if (StringUtils.isNotBlank(this.vestigingsnummer)) {
        zoekenParameters.vestigingsnummer = this.vestigingsnummer
    }
    if (StringUtils.isNotBlank(this.rsin)) {
        zoekenParameters.rsin = this.rsin
    }
    if (StringUtils.isNotBlank(this.naam)) {
        zoekenParameters.naam = this.naam
    }
    zoekenParameters.type = this.type?.type
    if (StringUtils.isNotBlank(this.postcode)) {
        zoekenParameters.postcode = this.postcode
    }
    zoekenParameters.huisnummer = this.huisnummer?.toString()
    return zoekenParameters
}
