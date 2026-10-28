/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.shared.model

import nl.info.zac.shared.model.SorteerRichting.ASCENDING
import nl.info.zac.shared.model.SorteerRichting.DESCENDING
import nl.info.zac.shared.model.SorteerRichting.NONE

enum class SorteerRichting(val value: String) {
    ASCENDING("asc"),
    DESCENDING("desc"),
    NONE("")
}

fun String?.toSorteerRichting(): SorteerRichting =
    when (this) {
        DESCENDING.value -> DESCENDING
        ASCENDING.value -> ASCENDING
        NONE.value -> NONE
        else -> throw IllegalArgumentException("Onbekende waarde '$this'")
    }
