/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.klant.model.bedrijven

import net.atos.zac.util.StringUtil
import nl.info.client.kvk.vestigingsprofiel.model.generated.Adres
import java.util.Locale
import nl.info.client.kvk.basisprofiel.model.generated.Adres as BasisprofielAdres

data class RestKlantenAdres(
    /**
     * Correspondentieadres en/of bezoekadres
     */
    var type: String,
    var afgeschermd: Boolean,
    var volledigAdres: String
)

fun Adres.toRestKlantenAdres() = RestKlantenAdres(
    this.type,
    this.indAfgeschermd?.isIndicatie() == true,
    toFormattedAddress(
        straatnaam, huisnummer, huisletter, huisnummerToevoeging,
        postcode, plaats, postbusnummer,
        straatHuisnummer, toevoegingAdres, postcodeWoonplaats, land
    )
)

fun BasisprofielAdres.toRestKlantenAdres() = RestKlantenAdres(
    this.type,
    this.indAfgeschermd?.isIndicatie() == true,
    toFormattedAddress(
        straatnaam, huisnummer, huisletter, huisnummerToevoeging,
        postcode, plaats, postbusnummer,
        straatHuisnummer, toevoegingAdres, postcodeWoonplaats, land
    )
)

fun String.isIndicatie(): Boolean =
    when (this.lowercase(Locale.getDefault())) {
        "ja" -> true
        "nee" -> false
        else -> error("Unexpected value: $this")
    }

private fun toFormattedAddress(
    straatnaam: String?,
    huisnummer: Int?,
    huisletter: String?,
    huisnummerToevoeging: String?,
    postcode: String?,
    plaats: String?,
    postbusnummer: Int?,
    straatHuisnummer: String?,
    toevoegingAdres: String?,
    postcodeWoonplaats: String?,
    land: String?
): String = when {
    postbusnummer != null -> StringUtil.joinNonBlankWith(
        ", ",
        "Postbus $postbusnummer",
        StringUtil.joinNonBlankWith(StringUtil.NON_BREAKING_SPACE, postcode, plaats)
            .replace(" ", StringUtil.NON_BREAKING_SPACE)
    )
    straatHuisnummer != null -> StringUtil.joinNonBlankWith(
        ", ",
        StringUtil.joinNonBlankWith(StringUtil.NON_BREAKING_SPACE, straatHuisnummer, toevoegingAdres)
            .replace(" ", StringUtil.NON_BREAKING_SPACE),
        postcodeWoonplaats?.replace(" ", StringUtil.NON_BREAKING_SPACE),
        land
    )
    else -> {
        val huisnummerStr = listOfNotNull(huisnummer?.toString(), huisletter).joinToString("")
        val streetPart = StringUtil.joinNonBlankWith(
            StringUtil.NON_BREAKING_SPACE, straatnaam, huisnummerStr, huisnummerToevoeging
        ).replace(" ", StringUtil.NON_BREAKING_SPACE)
        StringUtil.joinNonBlankWith(
            ", ",
            streetPart,
            StringUtil.joinNonBlankWith(StringUtil.NON_BREAKING_SPACE, postcode, plaats)
                .replace(" ", StringUtil.NON_BREAKING_SPACE)
        )
    }
}
