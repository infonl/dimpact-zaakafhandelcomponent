/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.kvk.util

import nl.info.client.kvk.zoeken.model.generated.ResultaatItem

fun ResultaatItem.toAddressString(): String {
    val binnenlandsAdres = this.getAdres()?.getBinnenlandsAdres() ?: return ""
    val huisnummerWithSuffix = listOfNotNull(
        binnenlandsAdres.getHuisnummer()?.toString(),
        binnenlandsAdres.getHuisletter()
    ).filter(String::isNotBlank).joinToString("")
    val streetLine = listOfNotNull(binnenlandsAdres.getStraatnaam(), huisnummerWithSuffix)
        .filter(String::isNotBlank)
        .joinToString(" ")
    val cityLine = listOfNotNull(binnenlandsAdres.getPostcode(), binnenlandsAdres.getPlaats())
        .filter(String::isNotBlank)
        .joinToString(" ")
    return listOf(streetLine, cityLine).filter(String::isNotBlank).joinToString(", ")
}
