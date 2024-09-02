/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zoeken

import net.atos.zac.app.zoeken.model.RESTDatumRange
import net.atos.zac.app.zoeken.model.RESTZoekParameters
import net.atos.zac.shared.model.SorteerRichting
import net.atos.zac.zoeken.model.DatumRange
import net.atos.zac.zoeken.model.DatumVeld
import net.atos.zac.zoeken.model.FilterParameters
import net.atos.zac.zoeken.model.FilterVeld
import net.atos.zac.zoeken.model.SorteerVeld
import net.atos.zac.zoeken.model.ZoekParameters
import net.atos.zac.zoeken.model.ZoekResultaat
import net.atos.zac.zoeken.model.ZoekVeld
import net.atos.zac.zoeken.model.index.ZoekObjectType
import net.atos.zac.zoeken.model.zoekobject.ZaakZoekObject
import java.util.EnumMap
import java.util.UUID

@Suppress("LongParameterList")
fun createRESTZoekParameters(
    type: ZoekObjectType = ZoekObjectType.ZAAK,
    zoeken: Map<String, String> = mapOf("dummyKey" to "dummyValue"),
    filters: Map<FilterVeld, FilterParameters> = mapOf(
        FilterVeld.BEHANDELAAR to FilterParameters(listOf("dummyFilterValue"), false)
    ),
    datums: Map<DatumVeld, RESTDatumRange> = mapOf(
        DatumVeld.STARTDATUM to RESTDatumRange()
    ),
    sorteerVeld: SorteerVeld = SorteerVeld.CREATED,
    sorteerRichting: String = "ASC",
    rows: Int = 0,
    page: Int = 0,
    alleenMijnZaken: Boolean = false,
    alleenOpenstaandeZaken: Boolean = false,
    alleenAfgeslotenZaken: Boolean = false,
    alleenMijnTaken: Boolean = false
) = RESTZoekParameters().apply {
    this.type = type
    this.zoeken = zoeken
    this.filters = filters
    this.datums = datums
    this.sorteerVeld = sorteerVeld
    this.sorteerRichting = sorteerRichting
    this.rows = rows
    this.page = page
    this.alleenMijnZaken = alleenMijnZaken
    this.alleenOpenstaandeZaken = alleenOpenstaandeZaken
    this.alleenAfgeslotenZaken = alleenAfgeslotenZaken
    this.alleenMijnTaken = alleenMijnTaken
}

fun createZaakZoekObject(
    uuid: UUID = UUID.randomUUID(),
    type: ZoekObjectType = ZoekObjectType.ZAAK,
    behandelaarGebruikersnaam: String = "dummyBehandelaarGebruikersnaam"
) = ZaakZoekObject().apply {
    this.uuid = uuid.toString()
    this.type = type
    this.behandelaarGebruikersnaam = behandelaarGebruikersnaam
}

@Suppress("LongParameterList")
fun createZoekParameters(
    rows: Int = 0,
    start: Int = 0,
    zoekObjectType: ZoekObjectType = ZoekObjectType.ZAAK,
    zoeken: EnumMap<ZoekVeld, String> = EnumMap<ZoekVeld, String>(ZoekVeld::class.java),
    datums: EnumMap<DatumVeld, DatumRange> = EnumMap<DatumVeld, DatumRange>(DatumVeld::class.java),
    sorteerVeld: SorteerVeld = SorteerVeld.CREATED,
    sorteerRichting: SorteerRichting = SorteerRichting.ASCENDING
) = ZoekParameters(zoekObjectType).apply {
    this.rows = rows
    this.start = start
    this.setZoeken(zoeken)
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
