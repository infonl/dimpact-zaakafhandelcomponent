/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.zoeken.model

import net.atos.zac.shared.model.SorteerRichting
import net.atos.zac.zoeken.model.index.ZoekObjectType
import java.util.EnumMap

class ZoekParameters(val type: ZoekObjectType?) {
    var rows: Int = 0

    var start: Int = 0

    private var zoeken = EnumMap<ZoekVeld, String>(ZoekVeld::class.java)

    var datums: EnumMap<DatumVeld, DatumRange> = EnumMap(DatumVeld::class.java)

    val filters: EnumMap<FilterVeld, FilterParameters> = EnumMap(
        FilterVeld::class.java
    )

    val filterQueries: HashMap<String, String> = HashMap()

    var sortering: Sortering = Sortering(SorteerVeld.CREATED, SorteerRichting.DESCENDING)
        private set

    init {
        beschikbareFilters.forEach { this.addFilter(it, FilterParameters(arrayListOf(), false)) }
    }

    fun getZoeken(): Map<ZoekVeld, String> {
        return zoeken
    }

    fun setZoeken(zoeken: EnumMap<ZoekVeld, String>) {
        this.zoeken = zoeken
    }

    fun addZoekVeld(zoekVeld: ZoekVeld, zoekTekst: String) {
        zoeken[zoekVeld] = zoekTekst
    }

    fun addDatum(zoekVeld: DatumVeld, range: DatumRange) {
        datums[zoekVeld] = range
    }

    fun addFilter(veld: FilterVeld, waarde: String) {
        filters[veld] = FilterParameters(listOf(waarde), false)
    }

    fun addFilter(veld: FilterVeld, filterParameters: FilterParameters) {
        filters[veld] = filterParameters
    }

    fun addFilterQuery(veld: String, waarde: String) {
        filterQueries[veld] = waarde
    }

    fun setSortering(veld: SorteerVeld, richting: SorteerRichting) {
        this.sortering = Sortering(veld, richting)
    }

    val isGlobaalZoeken: Boolean
        get() = this.type == null

    private val beschikbareFilters: Set<FilterVeld>
        get() {
            if (type == null) {
                return FilterVeld.facetten
            }
            return when (type) {
                ZoekObjectType.ZAAK -> FilterVeld.zaakFacetten
                ZoekObjectType.TAAK -> FilterVeld.taakFacetten
                ZoekObjectType.DOCUMENT -> FilterVeld.documentFacetten
            }
        }
}
