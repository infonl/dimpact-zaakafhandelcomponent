/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zoeken.model

import net.atos.zac.app.shared.RestPageParameters
import net.atos.zac.zoeken.model.DatumVeld
import net.atos.zac.zoeken.model.FilterParameters
import net.atos.zac.zoeken.model.FilterVeld
import net.atos.zac.zoeken.model.SorteerVeld
import net.atos.zac.zoeken.model.zoekobject.ZoekObjectType
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@Suppress("LongParameterList")
data class RestZoekParameters(
    override var page: Int,
    override var rows: Int,
    var type: ZoekObjectType? = null,
    var zoeken: MutableMap<String, String>? = null,
    var filters: MutableMap<FilterVeld, FilterParameters>? = null,
    var datums: MutableMap<DatumVeld, RestDatumRange>? = null,
    var sorteerVeld: SorteerVeld? = null,
    var sorteerRichting: String? = null,
    var alleenMijnZaken: Boolean = false,
    var alleenOpenstaandeZaken: Boolean = false,
    var alleenAfgeslotenZaken: Boolean = false,
    var alleenMijnTaken: Boolean = false
) : RestPageParameters(page, rows)
