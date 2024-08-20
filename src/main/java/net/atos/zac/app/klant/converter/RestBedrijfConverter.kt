/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.klant.converter

import net.atos.client.kvk.model.KvkZoekenParameters
import net.atos.client.kvk.zoeken.model.generated.Resultaat
import net.atos.client.kvk.zoeken.model.generated.ResultaatItem
import net.atos.zac.app.klant.model.bedrijven.RestBedrijf
import net.atos.zac.app.klant.model.bedrijven.RestListBedrijvenParameters
import net.atos.zac.util.StringUtil
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.StringUtils
import java.util.Locale
import java.util.Objects

fun toKvkZoekenParameters(restListParameters: RestListBedrijvenParameters): KvkZoekenParameters {
    val zoekenParameters = KvkZoekenParameters()
    if (StringUtils.isNotBlank(restListParameters.kvkNummer)) {
        zoekenParameters.kvkNummer = restListParameters.kvkNummer
    }
    if (StringUtils.isNotBlank(restListParameters.vestigingsnummer)) {
        zoekenParameters.vestigingsnummer = restListParameters.vestigingsnummer
    }
    if (StringUtils.isNotBlank(restListParameters.rsin)) {
        zoekenParameters.rsin = restListParameters.rsin
    }
    if (StringUtils.isNotBlank(restListParameters.naam)) {
        zoekenParameters.naam = restListParameters.naam
    }
    if (restListParameters.type != null) {
        zoekenParameters.type = restListParameters.type!!.type
    }
    if (StringUtils.isNotBlank(restListParameters.postcode)) {
        zoekenParameters.postcode = restListParameters.postcode
    }
    if (restListParameters.huisnummer != null) {
        zoekenParameters.huisnummer = restListParameters.huisnummer.toString()
    }
    return zoekenParameters
}

fun toRestBedrijven(resultaat: Resultaat): List<RestBedrijf> {
    if (CollectionUtils.isEmpty(resultaat.resultaten)) {
        return emptyList()
    }
    return resultaat.resultaten.map { it.toRestBedrijf() }
    // .map(Function<ResultaatItem, RestBedrijf> { obj: ResultaatItem? -> convert() })
}

fun ResultaatItem.toRestBedrijf() = RestBedrijf(
    kvkNummer = this.kvkNummer,
    vestigingsnummer = this.vestigingsnummer,
    naam = convertToNaam(this),
    postcode = this.adres.binnenlandsAdres.postcode,
    rsin = this.rsin,
    type = this.type.uppercase(Locale.getDefault()),
    adres = convertAdres(this)
)

private fun convertToNaam(bedrijf: ResultaatItem): String =
    StringUtils.replace(bedrijf.naam, StringUtils.SPACE, StringUtil.NON_BREAKING_SPACE)

private fun convertAdres(bedrijf: ResultaatItem): String {
    val binnenlandsAdres = bedrijf.adres.binnenlandsAdres
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
