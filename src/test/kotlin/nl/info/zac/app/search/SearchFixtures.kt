/*
 *
 *  * SPDX-FileCopyrightText: 2025 INFO.nl
 *  * SPDX-License-Identifier: EUPL-1.2+
 *
 */
package nl.info.zac.app.search

import nl.info.zac.app.search.model.RestDatumRange
import nl.info.zac.app.search.model.RestZoekParameters
import nl.info.zac.search.model.DatumRange
import nl.info.zac.search.model.DatumVeld
import nl.info.zac.search.model.FilterParameters
import nl.info.zac.search.model.FilterVeld
import nl.info.zac.search.model.SorteerVeld
import nl.info.zac.search.model.ZoekParameters
import nl.info.zac.search.model.ZoekResultaat
import nl.info.zac.search.model.createZaakZoekObject
import nl.info.zac.search.model.zoekobject.ZaakZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.shared.model.SorteerRichting
import java.util.EnumMap

@Suppress("LongParameterList")
fun createRESTZoekParameters(
    type: ZoekObjectType = ZoekObjectType.ZAAK,
    zoeken: Map<String, String> = mapOf("fakeKey" to "fakeValue"),
    filters: Map<FilterVeld, FilterParameters> = mapOf(
        FilterVeld.BEHANDELAAR to FilterParameters(listOf("fakeFilterValue"), false)
    ),
    datums: Map<DatumVeld, RestDatumRange> = mapOf(
        DatumVeld.STARTDATUM to RestDatumRange()
    ),
    sorteerVeld: SorteerVeld = SorteerVeld.CREATED,
    sorteerRichting: String = "ASC",
    rows: Int = 0,
    page: Int = 0,
    alleenMijnZaken: Boolean = false,
    alleenOpenstaandeZaken: Boolean = false,
    alleenAfgeslotenZaken: Boolean = false,
    alleenMijnTaken: Boolean = false
) = RestZoekParameters(
    type = type,
    zoeken = zoeken,
    filters = filters,
    datums = datums,
    sorteerVeld = sorteerVeld,
    sorteerRichting = sorteerRichting,
    rows = rows,
    page = page,
    alleenMijnZaken = alleenMijnZaken,
    alleenOpenstaandeZaken = alleenOpenstaandeZaken,
    alleenAfgeslotenZaken = alleenAfgeslotenZaken,
    alleenMijnTaken = alleenMijnTaken
)

@Suppress("LongParameterList")
fun createZoekParameters(
    rows: Int = 0,
    start: Int = 0,
    zoekObjectType: ZoekObjectType = ZoekObjectType.ZAAK,
    datums: EnumMap<DatumVeld, DatumRange> = EnumMap<DatumVeld, DatumRange>(DatumVeld::class.java),
    sorteerVeld: SorteerVeld = SorteerVeld.CREATED,
    sorteerRichting: SorteerRichting = SorteerRichting.ASCENDING
) = ZoekParameters(zoekObjectType).apply {
    this.rows = rows
    this.start = start
    this.datums = datums
    this.setSortering(sorteerVeld, sorteerRichting)
}

fun createZoekResultaatForZaakZoekObjecten(
    items: List<ZaakZoekObject> = listOf(createZaakZoekObject()),
    count: Long = 1L
) = ZoekResultaat(
    items,
    count
)
