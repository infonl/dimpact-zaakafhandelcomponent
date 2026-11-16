/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.search.model

import nl.info.zac.time.DateRange
import nl.info.zac.util.NoArgConstructor
import java.time.LocalDate

@NoArgConstructor
data class RestDateRange(
    var from: LocalDate? = null,
    var through: LocalDate? = null
) {
    fun toDateRange() = DateRange.of(from, through)
}
