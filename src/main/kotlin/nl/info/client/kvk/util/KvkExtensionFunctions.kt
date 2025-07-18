/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.kvk.util

import nl.info.client.kvk.zoeken.model.generated.ResultaatItem

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
