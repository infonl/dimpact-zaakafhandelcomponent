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
import nl.info.zac.util.joinNonBlank

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
    val huisnummerWithSuffix = listOf(
        this.getHuisnummer()?.toString(),
        this.getHuisletter(),
        this.getHuisnummertoevoeging()
    ).joinNonBlank()
    return listOf(
        listOf(this.getOfficieleStraatnaam(), huisnummerWithSuffix).joinNonBlank(" "),
        listOf(this.getPostcode(), this.getWoonplaats()).joinNonBlank(" ")
    ).joinNonBlank(", ")
}

private fun VerblijfadresBuitenland.toAddressString() =
    listOf(this.getRegel1(), this.getRegel2(), this.getRegel3()).joinNonBlank(", ")
