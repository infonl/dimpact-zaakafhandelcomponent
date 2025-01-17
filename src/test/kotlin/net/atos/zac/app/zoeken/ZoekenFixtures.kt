/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zoeken

import net.atos.zac.app.zoeken.model.RestDatumRange
import net.atos.zac.app.zoeken.model.RestZoekParameters
import net.atos.zac.shared.model.SorteerRichting
import net.atos.zac.zoeken.model.DatumRange
import net.atos.zac.zoeken.model.DatumVeld
import net.atos.zac.zoeken.model.FilterParameters
import net.atos.zac.zoeken.model.FilterVeld
import net.atos.zac.zoeken.model.SorteerVeld
import net.atos.zac.zoeken.model.ZoekParameters
import net.atos.zac.zoeken.model.ZoekResultaat
import net.atos.zac.zoeken.model.zoekobject.ZaakZoekObject
import net.atos.zac.zoeken.model.zoekobject.ZoekObjectType
import java.util.EnumMap
import java.util.UUID

@Suppress("LongParameterList")
fun createRESTZoekParameters(
    type: ZoekObjectType = ZoekObjectType.ZAAK,
    zoeken: Map<String, String> = mapOf("dummyKey" to "dummyValue"),
    filters: Map<FilterVeld, FilterParameters> = mapOf(
        FilterVeld.BEHANDELAAR to FilterParameters(listOf("dummyFilterValue"), false)
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

fun createZaakZoekObject(
    uuid: UUID = UUID.randomUUID(),
    type: ZoekObjectType = ZoekObjectType.ZAAK,
    behandelaarGebruikersnaam: String = "dummyBehandelaarGebruikersnaam"
) = ZaakZoekObject(
    id = uuid.toString(),
    type = type.name
).apply {
    this.behandelaarGebruikersnaam = behandelaarGebruikersnaam
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
