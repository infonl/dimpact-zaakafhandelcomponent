/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.brp.util

import nl.info.client.brp.model.generated.Adres
import nl.info.client.brp.model.generated.Persoon
import nl.info.client.brp.model.generated.VerblijfadresBinnenland
import nl.info.client.brp.model.generated.VerblijfadresBuitenland
import nl.info.client.brp.model.generated.VerblijfplaatsBuitenland

fun Persoon.toAddressString(): String {
    val verblijfplaats = this.getVerblijfplaats()
    return when (verblijfplaats) {
        is Adres if verblijfplaats.verblijfadres != null ->
            verblijfplaats.verblijfadres.toAddressString()
        is VerblijfplaatsBuitenland if verblijfplaats.verblijfadres != null ->
            verblijfplaats.verblijfadres.toAddressString()
        else -> ""
    }
}

private fun VerblijfadresBinnenland.toAddressString(): String {
    val huisnummerWithSuffix = listOfNotNull(
        this.getHuisnummer()?.toString(),
        this.getHuisletter(),
        this.getHuisnummertoevoeging()
    ).filter(String::isNotBlank).joinToString("")
    val streetLine = listOfNotNull(this.getOfficieleStraatnaam(), huisnummerWithSuffix)
        .filter(String::isNotBlank)
        .joinToString(" ")
    val cityLine = listOfNotNull(this.getPostcode(), this.getWoonplaats())
        .filter(String::isNotBlank)
        .joinToString(" ")
    return listOf(streetLine, cityLine).filter(String::isNotBlank).joinToString(", ")
}

private fun VerblijfadresBuitenland.toAddressString() =
    listOfNotNull(this.getRegel1(), this.getRegel2(), this.getRegel3())
        .filter(String::isNotBlank)
        .joinToString(", ")
