/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.kvk.util

import nl.info.client.kvk.zoeken.model.generated.ResultaatItem
import nl.info.zac.util.joinNonBlank

fun ResultaatItem.toAddressString(): String {
    val binnenlandsAdres = this.getAdres()?.getBinnenlandsAdres() ?: return ""
    val huisnummerWithSuffix = listOf(
        binnenlandsAdres.getHuisnummer()?.toString(),
        binnenlandsAdres.getHuisletter()
    ).joinNonBlank()
    return listOf(
        listOf(binnenlandsAdres.getStraatnaam(), huisnummerWithSuffix).joinNonBlank(" "),
        listOf(binnenlandsAdres.getPostcode(), binnenlandsAdres.getPlaats()).joinNonBlank(" ")
    ).joinNonBlank(", ")
}
