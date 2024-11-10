/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.zoeken.model

import net.atos.zac.shared.model.SorteerRichting
import net.atos.zac.zoeken.model.index.ZoekObjectType
import java.util.EnumMap

class ZoekParameters(val type: ZoekObjectType?) {
    private var zoeken = EnumMap<ZoekVeld, String>(ZoekVeld::class.java)
    var rows: Int = 0
    var start: Int = 0
    var datums = EnumMap<DatumVeld, DatumRange>(DatumVeld::class.java)
    val filters = EnumMap<FilterVeld, FilterParameters>(FilterVeld::class.java).apply {
        getBeschikbareFilters().forEach { this[it] = FilterParameters(arrayListOf(), false) }
    }
    val filterQueries = mutableMapOf<String, String>()
    var sortering = Sortering(SorteerVeld.CREATED, SorteerRichting.DESCENDING)

    fun getZoeken(): Map<ZoekVeld, String> = zoeken

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

    fun isGlobaalZoeken(): Boolean = this.type == null

    private fun getBeschikbareFilters(): Set<FilterVeld> {
        return when (type) {
            null -> FilterVeld.facetten
            ZoekObjectType.ZAAK -> FilterVeld.zaakFacetten
            ZoekObjectType.TAAK -> FilterVeld.taakFacetten
            ZoekObjectType.DOCUMENT -> FilterVeld.documentFacetten
        }
    }
}
