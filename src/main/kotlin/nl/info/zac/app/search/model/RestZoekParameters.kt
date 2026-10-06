/*
 *
 *  * SPDX-FileCopyrightText: 2025 INFO.nl
 *  * SPDX-License-Identifier: EUPL-1.2+
 *
 */
package nl.info.zac.app.search.model

import jakarta.json.bind.annotation.JsonbProperty
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import nl.info.zac.app.shared.RestPageParameters
import nl.info.zac.search.model.DatumVeld
import nl.info.zac.search.model.FilterParameters
import nl.info.zac.search.model.FilterVeld
import nl.info.zac.search.model.SorteerVeld
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@Suppress("LongParameterList")
data class RestZoekParameters(
    @field:PositiveOrZero
    override var page: Int,

    @field:Positive
    override var rows: Int,

    var type: ZoekObjectType? = null,
    var zoeken: Map<String, String>? = null,
    var filters: Map<FilterVeld, FilterParameters>? = null,
    var datums: Map<DatumVeld, RestDatumRange>? = null,
    var sorteerVeld: SorteerVeld? = null,
    var sorteerRichting: String? = null,

    @get:JsonbProperty("alleenMijnZaken")
    @set:JsonbProperty("alleenMijnZaken")
    var isAlleenMijnZaken: Boolean = false,

    @get:JsonbProperty("alleenOpenstaandeZaken")
    @set:JsonbProperty("alleenOpenstaandeZaken")
    var isAlleenOpenstaandeZaken: Boolean = false,

    @get:JsonbProperty("alleenAfgeslotenZaken")
    @set:JsonbProperty("alleenAfgeslotenZaken")
    var isAlleenAfgeslotenZaken: Boolean = false,

    @get:JsonbProperty("alleenMijnTaken")
    @set:JsonbProperty("alleenMijnTaken")
    var isAlleenMijnTaken: Boolean = false
) : RestPageParameters
