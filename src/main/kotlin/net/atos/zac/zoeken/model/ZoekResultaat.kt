/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.zoeken.model

import net.atos.zac.shared.model.Resultaat
import java.util.EnumMap

class ZoekResultaat<TYPE>(items: List<TYPE>, count: Long) : Resultaat<TYPE>(items, count) {
    private val filters = EnumMap<FilterVeld, MutableList<FilterResultaat>>(FilterVeld::class.java)

    fun addFilter(filterField: FilterVeld, filterResults: MutableList<FilterResultaat>) {
        filters[filterField] = filterResults
    }

    fun getFilters(): Map<FilterVeld, MutableList<FilterResultaat>> = filters
}
