/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.shared.model

import net.atos.zac.shared.model.SorteerRichting.DESCENDING
import org.apache.commons.lang3.StringUtils
import java.util.function.Supplier
import java.util.stream.Stream

enum class SorteerRichting(val value: String) {
    /** Oplopend  */
    ASCENDING("asc"),

    /** Aflopend  */
    DESCENDING("desc");
}

fun fromValue(waarde: String?): SorteerRichting =
    when {
        waarde.isNullOrBlank() -> DESCENDING
        else -> SorteerRichting.entries.toTypedArray().firstOrNull { it.value.toString() == waarde }
            ?: throw IllegalArgumentException("Onbekende waarde '$waarde'")
    }
