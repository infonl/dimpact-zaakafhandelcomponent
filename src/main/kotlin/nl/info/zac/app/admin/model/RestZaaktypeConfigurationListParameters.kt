/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import jakarta.json.bind.annotation.JsonbProperty
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import nl.info.zac.app.search.model.RestDatumRange
import nl.info.zac.app.shared.RestPageParameters
import nl.info.zac.search.model.SorteerRichting
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

/**
 * A page of the Zaakafhandelparameters page, with its filters and its sort order. A filter that is `null` does not
 * filter. Without a sort field, the page keeps the order of the catalogus.
 */
@NoArgConstructor
@AllOpen
@Suppress("LongParameterList")
data class RestZaaktypeConfigurationListParameters(
    @field:PositiveOrZero
    override var page: Int = 0,

    @field:Positive
    override var rows: Int = DEFAULT_ROWS,

    var sortField: RestZaaktypeConfigurationSortField? = null,

    var sortOrder: SorteerRichting = SorteerRichting.ASC,

    @get:JsonbProperty("isValide")
    @set:JsonbProperty("isValide")
    var isValide: Boolean? = null,

    @get:JsonbProperty("isNuGeldig")
    @set:JsonbProperty("isNuGeldig")
    var isNuGeldig: Boolean? = null,

    var zaaktypeIdentificatie: String? = null,

    var processDefinitionKey: String? = null,

    var beginGeldigheid: RestDatumRange? = null,

    var eindeGeldigheid: RestDatumRange? = null
) : RestPageParameters {
    companion object {
        const val DEFAULT_ROWS = 25
    }
}
