/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.klant.model.bedrijven

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.client.kvk.zoeken.model.generated.BinnenlandsAdres
import nl.info.client.kvk.zoeken.model.generated.BuitenlandsAdres
import nl.info.zac.util.NON_BREAKING_SPACE
import nl.info.zac.util.joinNonBlank
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
    postbusnummer != null -> listOf(
        "Postbus $postbusnummer",
        listOf(postcode, plaats).joinNonBlank(NON_BREAKING_SPACE)
            .replace(" ", NON_BREAKING_SPACE)
    ).joinNonBlank(", ")
    else -> {
        val huisnummerStr = listOfNotNull(huisnummer?.toString(), huisletter).joinToString("")
        listOf(
            listOf(straatnaam, huisnummerStr).joinNonBlank(NON_BREAKING_SPACE)
                .replace(" ", NON_BREAKING_SPACE),
            listOf(postcode, plaats).joinNonBlank(NON_BREAKING_SPACE)
                .replace(" ", NON_BREAKING_SPACE)
        ).joinNonBlank(", ")
    }
}

internal fun BuitenlandsAdres.toFormattedAddress(): String = listOf(
    straatHuisnummer?.replace(" ", NON_BREAKING_SPACE),
    postcodeWoonplaats?.replace(" ", NON_BREAKING_SPACE),
    land
).joinNonBlank(", ")

internal fun BasisprofielAdres.toFormattedAddress(): String = when {
    postbusnummer != null -> listOf(
        "Postbus $postbusnummer",
        listOf(postcode, plaats).joinNonBlank(NON_BREAKING_SPACE)
            .replace(" ", NON_BREAKING_SPACE)
    ).joinNonBlank(", ")
    straatHuisnummer != null -> listOf(
        listOf(straatHuisnummer, toevoegingAdres).joinNonBlank(NON_BREAKING_SPACE)
            .replace(" ", NON_BREAKING_SPACE),
        postcodeWoonplaats?.replace(" ", NON_BREAKING_SPACE),
        land
    ).joinNonBlank(", ")
    else -> {
        val huisnummerStr = listOfNotNull(huisnummer?.toString(), huisletter).joinToString("")
        listOf(
            listOf(straatnaam, huisnummerStr, huisnummerToevoeging).joinNonBlank(NON_BREAKING_SPACE)
                .replace(" ", NON_BREAKING_SPACE),
            listOf(postcode, plaats).joinNonBlank(NON_BREAKING_SPACE)
                .replace(" ", NON_BREAKING_SPACE)
        ).joinNonBlank(", ")
    }
}

internal fun VestigingsprofielAdres.toFormattedAddress(): String = when {
    postbusnummer != null -> listOf(
        "Postbus $postbusnummer",
        listOf(postcode, plaats).joinNonBlank(NON_BREAKING_SPACE)
            .replace(" ", NON_BREAKING_SPACE)
    ).joinNonBlank(", ")
    straatHuisnummer != null -> listOf(
        listOf(straatHuisnummer, toevoegingAdres).joinNonBlank(NON_BREAKING_SPACE)
            .replace(" ", NON_BREAKING_SPACE),
        postcodeWoonplaats?.replace(" ", NON_BREAKING_SPACE),
        land
    ).joinNonBlank(", ")
    else -> {
        val huisnummerStr = listOfNotNull(huisnummer?.toString(), huisletter).joinToString("")
        listOf(
            listOf(straatnaam, huisnummerStr, huisnummerToevoeging).joinNonBlank(NON_BREAKING_SPACE)
                .replace(" ", NON_BREAKING_SPACE),
            listOf(postcode, plaats).joinNonBlank(NON_BREAKING_SPACE)
                .replace(" ", NON_BREAKING_SPACE)
        ).joinNonBlank(", ")
    }
}
