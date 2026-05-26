/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.klant.model.bedrijven

import net.atos.zac.util.StringUtil
import java.util.Locale
import nl.info.client.kvk.basisprofiel.model.generated.Adres as BasisprofielAdres
import nl.info.client.kvk.vestigingsprofiel.model.generated.Adres as VestigingsprofielAdres
import nl.info.client.kvk.zoeken.model.generated.BinnenlandsAdres
import nl.info.client.kvk.zoeken.model.generated.BuitenlandsAdres

data class RestKlantenAdres(
    /**
     * Correspondentieadres en/of bezoekadres
     */
    var type: String,
    var afgeschermd: Boolean,
    var volledigAdres: String
)

fun VestigingsprofielAdres.toRestKlantenAdres() = RestKlantenAdres(
    this.type,
    this.indAfgeschermd?.isIndicatie() == true,
    this.toKvkAdres().toFormattedAddress()
)

fun BasisprofielAdres.toRestKlantenAdres() = RestKlantenAdres(
    this.type,
    this.indAfgeschermd?.isIndicatie() == true,
    this.toKvkAdres().toFormattedAddress()
)

fun String.isIndicatie(): Boolean =
    when (this.lowercase(Locale.getDefault())) {
        "ja" -> true
        "nee" -> false
        else -> error("Unexpected value: $this")
    }

internal data class KvkAdres(
    val postbusnummer: Int?,
    val straatHuisnummer: String?,
    val toevoegingAdres: String?,
    val postcodeWoonplaats: String?,
    val land: String?,
    val straatnaam: String?,
    val huisnummer: Int?,
    val huisletter: String?,
    val huisnummerToevoeging: String?,
    val postcode: String?,
    val plaats: String?
)

internal fun VestigingsprofielAdres.toKvkAdres() = KvkAdres(
    postbusnummer, straatHuisnummer, toevoegingAdres, postcodeWoonplaats, land,
    straatnaam, huisnummer, huisletter, huisnummerToevoeging, postcode, plaats
)

internal fun BasisprofielAdres.toKvkAdres() = KvkAdres(
    postbusnummer, straatHuisnummer, toevoegingAdres, postcodeWoonplaats, land,
    straatnaam, huisnummer, huisletter, huisnummerToevoeging, postcode, plaats
)

internal fun BinnenlandsAdres.toKvkAdres() = KvkAdres(
    postbusnummer = postbusnummer,
    straatHuisnummer = null,
    toevoegingAdres = null,
    postcodeWoonplaats = null,
    land = null,
    straatnaam = straatnaam,
    huisnummer = huisnummer,
    huisletter = huisletter,
    huisnummerToevoeging = null,
    postcode = postcode,
    plaats = plaats
)

internal fun BuitenlandsAdres.toKvkAdres() = KvkAdres(
    postbusnummer = null,
    straatHuisnummer = straatHuisnummer,
    toevoegingAdres = null,
    postcodeWoonplaats = postcodeWoonplaats,
    land = land,
    straatnaam = null,
    huisnummer = null,
    huisletter = null,
    huisnummerToevoeging = null,
    postcode = null,
    plaats = null
)

internal fun KvkAdres.toFormattedAddress(): String = when {
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
