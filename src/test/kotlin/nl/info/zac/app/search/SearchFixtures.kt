/*
 *
 *  * SPDX-FileCopyrightText: 2025 INFO.nl
 *  * SPDX-License-Identifier: EUPL-1.2+
 *
 */
package nl.info.zac.app.search

import nl.info.zac.app.search.model.RestDatumRange
import nl.info.zac.app.search.model.RestDocumentZoekObject
import nl.info.zac.app.search.model.RestTaakZoekObject
import nl.info.zac.app.search.model.RestZaakKoppelenZoekObject
import nl.info.zac.app.search.model.RestZaakZoekObject
import nl.info.zac.app.search.model.RestZoekKoppelenParameters
import nl.info.zac.app.search.model.RestZoekParameters
import nl.info.zac.app.search.model.RestZoekResultaat
import nl.info.zac.search.model.DatumRange
import nl.info.zac.search.model.DatumVeld
import nl.info.zac.search.model.FilterParameters
import nl.info.zac.search.model.FilterResultaat
import nl.info.zac.search.model.FilterVeld
import nl.info.zac.search.model.SorteerVeld
import nl.info.zac.search.model.ZoekParameters
import nl.info.zac.search.model.ZoekResultaat
import nl.info.zac.search.model.createZaakZoekObject
import nl.info.zac.search.model.zoekobject.ZaakZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.shared.model.SorteerRichting
import java.util.EnumMap
import java.util.UUID

fun createRestDocumentZoekObject(
    id: String = "fakeId",
    type: ZoekObjectType = ZoekObjectType.DOCUMENT,
    identificatie: String = "fakeIdentificatie",
) = RestDocumentZoekObject(
    id = id,
    type = type,
    identificatie = identificatie
)

fun createRestTaakZoekObject(
    id: String = "fakeId",
    type: ZoekObjectType = ZoekObjectType.TAAK,
    identificatie: String = "fakeIdentificatie",
) = RestTaakZoekObject(
    id = id,
    type = type,
    identificatie = identificatie
)

fun createRestZaakKoppelenZoekObject(
    id: String = "fakeId",
    type: ZoekObjectType = ZoekObjectType.ZAAK,
    identificatie: String = "fakeIdentificatie",
) = RestZaakKoppelenZoekObject(
    id = id,
    type = type,
    identificatie = identificatie
)

fun createRestZaakZoekObject(
    id: String = "fakeId",
    type: ZoekObjectType = ZoekObjectType.ZAAK,
    identificatie: String = "fakeIdentificatie",
) = RestZaakZoekObject(
    id = id,
    type = type,
    identificatie = identificatie
)

@Suppress("LongParameterList")
fun createRestZoekParameters(
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

fun createRestZoekKoppelenParameters(
    page: Int = 0,
    rows: Int = 10,
    zaakIdentificator: String = "fakeZaakIdentificator",
    informationObjectTypeUuid: UUID = UUID.randomUUID()
) = RestZoekKoppelenParameters(
    page = page,
    rows = rows,
    zaakIdentificator = zaakIdentificator,
    informationObjectTypeUuid = informationObjectTypeUuid
)

fun createRestZoekResultaatForDocumentZoekObjects(
    items: List<RestDocumentZoekObject> = listOf(createRestDocumentZoekObject()),
    count: Long = 1L,
    filters: Map<out FilterVeld, MutableList<FilterResultaat>> = emptyMap()
) = RestZoekResultaat(
    items,
    count
).apply {
    this.filters.putAll(filters)
}

fun createRestZoekResultaatForTaakZoekObjects(
    items: List<RestTaakZoekObject> = listOf(createRestTaakZoekObject()),
    count: Long = 1L,
    filters: Map<out FilterVeld, MutableList<FilterResultaat>> = emptyMap()
) = RestZoekResultaat(
    items,
    count
).apply {
    this.filters.putAll(filters)
}

fun createRestZoekResultaatForZaakKoppelenZoekObjects(
    items: List<RestZaakKoppelenZoekObject> = listOf(createRestZaakKoppelenZoekObject()),
    count: Long = 1L,
    filters: Map<out FilterVeld, MutableList<FilterResultaat>> = emptyMap()
) = RestZoekResultaat(
    items,
    count
).apply {
    this.filters.putAll(filters)
}

fun createRestZoekResultaatForZaakZoekObjects(
    items: List<RestZaakZoekObject> = listOf(createRestZaakZoekObject()),
    count: Long = 1L,
    filters: Map<out FilterVeld, MutableList<FilterResultaat>> = emptyMap()
) = RestZoekResultaat(
    items,
    count
).apply {
    this.filters.putAll(filters)
}

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
