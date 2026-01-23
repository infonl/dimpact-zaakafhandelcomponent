/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
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

fun fromValue(waarde: String?): SorteerRichting =
    when {
        DESCENDING.value == waarde -> DESCENDING
        ASCENDING.value == waarde -> ASCENDING
        NONE.value == waarde -> NONE
        else -> throw IllegalArgumentException("Onbekende waarde '$waarde'")
    }
