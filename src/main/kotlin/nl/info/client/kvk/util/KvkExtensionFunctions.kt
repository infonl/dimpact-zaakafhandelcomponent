/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.kvk.util

import net.atos.zac.util.StringUtil.joinNonBlank
import net.atos.zac.util.StringUtil.joinNonBlankWith
import nl.info.client.kvk.zoeken.model.generated.ResultaatItem

fun ResultaatItem.toAddressString(): String =
    this.getAdres()?.getBinnenlandsAdres()?.let {
        joinNonBlankWith(
            ", ",
            joinNonBlankWith(" ", it.getStraatnaam(), joinNonBlank(it.getHuisnummer()?.toString(), it.getHuisletter())),
            joinNonBlankWith(" ", it.getPostcode(), it.getPlaats())
        )
    }.orEmpty()
