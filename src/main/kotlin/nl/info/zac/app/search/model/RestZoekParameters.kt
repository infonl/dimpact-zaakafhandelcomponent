/*
 *
 *  * SPDX-FileCopyrightText: 2025 Lifely
 *  * SPDX-License-Identifier: EUPL-1.2+
 *
 */
package nl.info.zac.app.search.model

import net.atos.zac.app.shared.RestPageParameters
import nl.info.zac.search.model.DatumVeld
import nl.info.zac.search.model.FilterParameters
import nl.info.zac.search.model.FilterVeld
import nl.info.zac.search.model.SorteerVeld
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@Suppress("LongParameterList")
data class RestZoekParameters(
    override var page: Int,
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
