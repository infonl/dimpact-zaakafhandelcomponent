/*
 *
 *  * SPDX-FileCopyrightText: 2025 INFO.nl
 *  * SPDX-License-Identifier: EUPL-1.2+
 *
 */
package nl.info.zac.app.search.model

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

    @field: Positive
    override var rows: Int,

    var type: ZoekObjectType? = null,
    var zoeken: Map<String, String>? = null,
    var filters: Map<FilterVeld, FilterParameters>? = null,
    var datums: Map<DatumVeld, RestDatumRange>? = null,
    var sorteerVeld: SorteerVeld? = null,
    var sorteerRichting: String? = null,
    var alleenMijnZaken: Boolean = false,
    var alleenOpenstaandeZaken: Boolean = false,
    var alleenAfgeslotenZaken: Boolean = false,
    var alleenMijnTaken: Boolean = false
) : RestPageParameters(page, rows)
