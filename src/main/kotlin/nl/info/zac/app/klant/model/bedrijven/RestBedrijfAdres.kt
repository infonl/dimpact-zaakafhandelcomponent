/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.klant.model.bedrijven

import jakarta.json.bind.annotation.JsonbProperty
import net.atos.zac.util.StringUtil
import net.atos.zac.util.StringUtil.NON_BREAKING_SPACE
import nl.info.client.kvk.zoeken.model.generated.BinnenlandsAdres
import nl.info.client.kvk.zoeken.model.generated.BuitenlandsAdres
import java.util.Locale
import nl.info.client.kvk.basisprofiel.model.generated.Adres as BasisprofielAdres
import nl.info.client.kvk.vestigingsprofiel.model.generated.Adres as VestigingsprofielAdres

data class RestBedrijfAdres(
    /**
     * Correspondentieadres en/of bezoekadres
     */
    var type: String,

    @get:JsonbProperty("isAfgeschermd")
    @set:JsonbProperty("isAfgeschermd")
    var isAfgeschermd: Boolean,

    var volledigAdres: String,
    var postcode: String? = null
)

fun VestigingsprofielAdres.toRestBedrijfAdres() = RestBedrijfAdres(
    type = this.type,
    isAfgeschermd = this.indAfgeschermd?.isIndicatie() == true,
    volledigAdres = this.toFormattedAddress().ifBlank { this.volledigAdres ?: "" },
    postcode = this.postcode
)

fun BasisprofielAdres.toRestBedrijfAdres() = RestBedrijfAdres(
    type = this.type,
    isAfgeschermd = this.indAfgeschermd?.isIndicatie() == true,
    volledigAdres = this.toFormattedAddress().ifBlank { this.volledigAdres ?: "" },
    postcode = this.postcode
)

fun String.isIndicatie(): Boolean =
    when (this.lowercase(Locale.getDefault())) {
        "ja" -> true
        "nee" -> false
        else -> error("Unexpected value: $this")
    }

internal fun BinnenlandsAdres.toFormattedAddress(): String = when {
    postbusnummer != null -> StringUtil.joinNonBlankWith(
        ", ",
        "Postbus $postbusnummer",
        StringUtil.joinNonBlankWith(NON_BREAKING_SPACE, postcode, plaats)
            .replace(" ", NON_BREAKING_SPACE)
    )
    else -> {
        val huisnummerStr = listOfNotNull(huisnummer?.toString(), huisletter).joinToString("")
        StringUtil.joinNonBlankWith(
            ", ",
            StringUtil.joinNonBlankWith(NON_BREAKING_SPACE, straatnaam, huisnummerStr)
                .replace(" ", NON_BREAKING_SPACE),
            StringUtil.joinNonBlankWith(NON_BREAKING_SPACE, postcode, plaats)
                .replace(" ", NON_BREAKING_SPACE)
        )
    }
}

internal fun BuitenlandsAdres.toFormattedAddress(): String = StringUtil.joinNonBlankWith(
    ", ",
    straatHuisnummer?.replace(" ", NON_BREAKING_SPACE),
    postcodeWoonplaats?.replace(" ", NON_BREAKING_SPACE),
    land
)

internal fun BasisprofielAdres.toFormattedAddress(): String = when {
    postbusnummer != null -> StringUtil.joinNonBlankWith(
        ", ",
        "Postbus $postbusnummer",
        StringUtil.joinNonBlankWith(NON_BREAKING_SPACE, postcode, plaats)
            .replace(" ", NON_BREAKING_SPACE)
    )
    straatHuisnummer != null -> StringUtil.joinNonBlankWith(
        ", ",
        StringUtil.joinNonBlankWith(NON_BREAKING_SPACE, straatHuisnummer, toevoegingAdres)
            .replace(" ", NON_BREAKING_SPACE),
        postcodeWoonplaats?.replace(" ", NON_BREAKING_SPACE),
        land
    )
    else -> {
        val huisnummerStr = listOfNotNull(huisnummer?.toString(), huisletter).joinToString("")
        StringUtil.joinNonBlankWith(
            ", ",
            StringUtil.joinNonBlankWith(NON_BREAKING_SPACE, straatnaam, huisnummerStr, huisnummerToevoeging)
                .replace(" ", NON_BREAKING_SPACE),
            StringUtil.joinNonBlankWith(NON_BREAKING_SPACE, postcode, plaats)
                .replace(" ", NON_BREAKING_SPACE)
        )
    }
}

internal fun VestigingsprofielAdres.toFormattedAddress(): String = when {
    postbusnummer != null -> StringUtil.joinNonBlankWith(
        ", ",
        "Postbus $postbusnummer",
        StringUtil.joinNonBlankWith(NON_BREAKING_SPACE, postcode, plaats)
            .replace(" ", NON_BREAKING_SPACE)
    )
    straatHuisnummer != null -> StringUtil.joinNonBlankWith(
        ", ",
        StringUtil.joinNonBlankWith(NON_BREAKING_SPACE, straatHuisnummer, toevoegingAdres)
            .replace(" ", NON_BREAKING_SPACE),
        postcodeWoonplaats?.replace(" ", NON_BREAKING_SPACE),
        land
    )
    else -> {
        val huisnummerStr = listOfNotNull(huisnummer?.toString(), huisletter).joinToString("")
        StringUtil.joinNonBlankWith(
            ", ",
            StringUtil.joinNonBlankWith(NON_BREAKING_SPACE, straatnaam, huisnummerStr, huisnummerToevoeging)
                .replace(" ", NON_BREAKING_SPACE),
            StringUtil.joinNonBlankWith(NON_BREAKING_SPACE, postcode, plaats)
                .replace(" ", NON_BREAKING_SPACE)
        )
    }
}
