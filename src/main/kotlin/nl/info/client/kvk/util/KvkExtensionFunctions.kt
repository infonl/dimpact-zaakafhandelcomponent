/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.kvk.util

import net.atos.zac.util.StringUtil
import nl.info.client.brp.model.generated.Adres
import nl.info.client.brp.model.generated.Persoon
import nl.info.client.brp.model.generated.VerblijfadresBinnenland
import nl.info.client.brp.model.generated.VerblijfadresBuitenland
import nl.info.client.brp.model.generated.VerblijfplaatsBuitenland
import nl.info.client.kvk.zoeken.model.generated.ResultaatItem

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

fun ResultaatItem.toAddressString(): String {
    val binnenlandsAdres = this.getAdres().getBinnenlandsAdres()
    return binnenlandsAdres.getStraatnaam() +
        " " +
        (binnenlandsAdres.getHuisnummer() ?: "") +
        (binnenlandsAdres.getHuisletter()?.takeIf { it.isNotBlank() } ?: "") +
        ", " +
        (binnenlandsAdres.getPostcode()?.takeIf { it.isNotBlank() } ?: "") +
        " " +
        binnenlandsAdres.getPlaats()
}

private fun VerblijfadresBinnenland.toAddressString() =
    (this.getOfficieleStraatnaam()?.takeIf { it.isNotBlank() } ?: "") +
        " " +
        (this.getHuisnummer() ?: "") +
        (this.getHuisletter()?.takeIf { it.isNotBlank() } ?: "") +
        (this.getHuisnummertoevoeging()?.takeIf { it.isNotBlank() } ?: "") +
        ", " +
        (this.getPostcode()?.takeIf { it.isNotBlank() } ?: "") +
        " " +
        this.getWoonplaats()

private fun VerblijfadresBuitenland.toAddressString(): String =
    StringUtil.joinNonBlankWith(", ", this.getRegel1(), this.getRegel2(), this.getRegel3())
