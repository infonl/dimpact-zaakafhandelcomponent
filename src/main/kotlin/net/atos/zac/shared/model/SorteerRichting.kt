/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.shared.model

import net.atos.zac.shared.model.SorteerRichting.ASCENDING
import net.atos.zac.shared.model.SorteerRichting.DESCENDING

enum class SorteerRichting(val value: String) {
    ASCENDING("asc"),
    DESCENDING("desc")
}

fun fromValue(waarde: String?): SorteerRichting =
    when {
        DESCENDING.value == waarde -> DESCENDING
        ASCENDING.value == waarde -> ASCENDING
        else -> throw IllegalArgumentException("Onbekende waarde '$waarde'")
    }
