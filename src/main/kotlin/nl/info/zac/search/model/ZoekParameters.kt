/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.model

import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.shared.model.SorteerRichting
import java.util.EnumMap

@Suppress("TooManyFunctions")
class ZoekParameters(val type: ZoekObjectType?) {
    private val zoeken = EnumMap<ZoekVeld, String>(ZoekVeld::class.java)
    private val filters = EnumMap<FilterVeld, FilterParameters>(FilterVeld::class.java).apply {
        getAvailableFilterFields(type).forEach { this[it] = FilterParameters(arrayListOf(), false) }
    }
    private val filterQueries = mutableMapOf<String, String>()

    var rows: Int = 0
    var start: Int = 0
    var datums = EnumMap<DatumVeld, DatumRange>(DatumVeld::class.java)
    var sortering = Sortering(SorteerVeld.CREATED, SorteerRichting.DESCENDING)

    fun getZoeken(): Map<ZoekVeld, String> = zoeken

    fun addZoekVeld(zoekVeld: ZoekVeld, zoekTekst: String) {
        zoeken[zoekVeld] = zoekTekst
    }

    fun addDatum(zoekVeld: DatumVeld, range: DatumRange) {
        datums[zoekVeld] = range
    }

    fun getFilters(): Map<FilterVeld, FilterParameters> = filters

    fun addFilter(filterVeld: FilterVeld, value: String) {
        filters[filterVeld] = FilterParameters(listOf(value), false)
    }

    fun addFilter(filterVeld: FilterVeld, filterParameters: FilterParameters) {
        filters[filterVeld] = filterParameters
    }

    fun getFilterQueries(): Map<String, String> = filterQueries

    fun addFilterQuery(veld: String, waarde: String) {
        filterQueries[veld] = waarde
    }

    fun setSortering(sorteerVeld: SorteerVeld, sorteerRichting: SorteerRichting) {
        this.sortering = Sortering(sorteerVeld, sorteerRichting)
    }

    fun isGlobaalZoeken(): Boolean = this.type == null

    private fun getAvailableFilterFields(type: ZoekObjectType?): Set<FilterVeld> =
        when (type) {
            null -> FilterVeld.facetten
            ZoekObjectType.ZAAK -> FilterVeld.zaakFacetten
            ZoekObjectType.TAAK -> FilterVeld.taakFacetten
            ZoekObjectType.DOCUMENT -> FilterVeld.documentFacetten
        }
}
