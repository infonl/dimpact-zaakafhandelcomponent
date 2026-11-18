/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.QueryParam
import nl.info.zac.app.shared.RestPageParameters
import nl.info.zac.search.model.SorteerRichting
import java.time.LocalDate

// Not a data class: RESTEasy fills a @BeanParam through a no-arg constructor that must run the initializers.
/**
 * The query parameters of a page of the Zaakafhandelparameters page, with its filters and its sort order. A filter
 * that is not given does not filter. Without a sort field, the page keeps the order of the catalogus.
 */
class RestZaaktypeConfigurationListParameters : RestPageParameters {
    companion object {
        const val DEFAULT_ROWS = 25
    }

    @field:QueryParam("page")
    @field:DefaultValue("0")
    @field:PositiveOrZero
    override var page: Int = 0

    @field:QueryParam("rows")
    @field:DefaultValue(DEFAULT_ROWS.toString())
    @field:Positive
    override var rows: Int = DEFAULT_ROWS

    @field:QueryParam("sortField")
    var sortField: RestZaaktypeConfigurationSortField? = null

    @field:QueryParam("sortOrder")
    @field:DefaultValue("ASC")
    var sortOrder: SorteerRichting = SorteerRichting.ASC

    @field:QueryParam("isValide")
    var isValide: Boolean? = null

    @field:QueryParam("isNuGeldig")
    var isNuGeldig: Boolean? = null

    @field:QueryParam("zaaktypeIdentificatie")
    var zaaktypeIdentificatie: String? = null

    @field:QueryParam("processDefinitionKey")
    var processDefinitionKey: String? = null

    @field:QueryParam("beginGeldigheidVan")
    var beginGeldigheidVan: LocalDate? = null

    @field:QueryParam("beginGeldigheidTot")
    var beginGeldigheidTot: LocalDate? = null

    @field:QueryParam("eindeGeldigheidVan")
    var eindeGeldigheidVan: LocalDate? = null

    @field:QueryParam("eindeGeldigheidTot")
    var eindeGeldigheidTot: LocalDate? = null
}
