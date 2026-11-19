/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.QueryParam
import nl.info.zac.app.shared.RestPageParameters
import nl.info.zac.search.model.SorteerRichting
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RestInrichtingscheckParameters(
    @field:QueryParam("page")
    @field:DefaultValue("0")
    @field:PositiveOrZero
    override var page: Int = 0,

    @field:QueryParam("rows")
    @field:DefaultValue(DEFAULT_ROWS.toString())
    @field:Positive
    @field:Max(MAX_ROWS)
    override var rows: Int = DEFAULT_ROWS,

    @field:QueryParam("zoekterm")
    var zoekterm: String? = null,

    @field:QueryParam("status")
    var status: RestInrichtingscheckStatus? = null,

    @field:QueryParam("sortField")
    @field:DefaultValue("OMSCHRIJVING")
    var sortField: RestInrichtingscheckSortField = RestInrichtingscheckSortField.OMSCHRIJVING,

    @field:QueryParam("sortOrder")
    @field:DefaultValue("ASC")
    var sortOrder: SorteerRichting = SorteerRichting.ASC
) : RestPageParameters {
    companion object {
        const val DEFAULT_ROWS = 25
        const val MAX_ROWS = 100L
    }
}
