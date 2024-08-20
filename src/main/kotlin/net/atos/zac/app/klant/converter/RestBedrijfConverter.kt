/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant.converter

import net.atos.client.kvk.model.KvkZoekenParameters
import net.atos.client.kvk.zoeken.model.generated.ResultaatItem
import net.atos.zac.app.klant.model.bedrijven.RestBedrijf
import net.atos.zac.app.klant.model.bedrijven.RestListBedrijvenParameters
import net.atos.zac.util.StringUtil
import org.apache.commons.lang3.StringUtils
import java.util.Locale
import java.util.Objects

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

fun ResultaatItem.toRestBedrijf() = RestBedrijf(
    kvkNummer = this.kvkNummer,
    vestigingsnummer = this.vestigingsnummer,
    naam = this.toName(),
    postcode = this.adres.binnenlandsAdres.postcode,
    rsin = this.rsin,
    type = this.type.uppercase(Locale.getDefault()),
    adres = this.toAddress()
)

private fun ResultaatItem.toName(): String =
    StringUtils.replace(this.naam, StringUtils.SPACE, StringUtil.NON_BREAKING_SPACE)

private fun ResultaatItem.toAddress(): String {
    val binnenlandsAdres = this.adres.binnenlandsAdres
    val adres = StringUtils.replace(
        StringUtil.joinNonBlankWith(
            StringUtil.NON_BREAKING_SPACE,
            binnenlandsAdres.straatnaam,
            Objects.toString(binnenlandsAdres.huisnummer, null),
            binnenlandsAdres.huisletter
        ),
        StringUtils.SPACE,
        StringUtil.NON_BREAKING_SPACE
    )
    val postcode = StringUtils.replace(binnenlandsAdres.postcode, StringUtils.SPACE, StringUtil.NON_BREAKING_SPACE)
    val woonplaats = StringUtils.replace(binnenlandsAdres.plaats, StringUtils.SPACE, StringUtil.NON_BREAKING_SPACE)
    return StringUtil.joinNonBlankWith(", ", adres, postcode, woonplaats)
}
