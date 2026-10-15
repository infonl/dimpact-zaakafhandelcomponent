/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.kvk.util

import nl.info.client.kvk.zoeken.model.generated.BinnenlandsAdres
import nl.info.client.kvk.zoeken.model.generated.BuitenlandsAdres
import nl.info.client.kvk.zoeken.model.generated.ResultaatItem
import nl.info.zac.util.joinNonBlank

fun ResultaatItem.toAddressString() =
    this.getAdres()?.getBinnenlandsAdres()?.toAddressString()
        ?: this.getAdres()?.getBuitenlandsAdres()?.toAddressString().orEmpty()

private fun BinnenlandsAdres.toAddressString(): String {
    val huisnummerWithSuffix = listOf(this.getHuisnummer()?.toString(), this.getHuisletter()).joinNonBlank()
    return listOf(
        listOf(this.getStraatnaam(), huisnummerWithSuffix).joinNonBlank(" "),
        listOf(this.getPostcode(), this.getPlaats()).joinNonBlank(" ")
    ).joinNonBlank(", ")
}

private fun BuitenlandsAdres.toAddressString() =
    listOf(this.getStraatHuisnummer(), this.getPostcodeWoonplaats(), this.getLand()).joinNonBlank(", ")
