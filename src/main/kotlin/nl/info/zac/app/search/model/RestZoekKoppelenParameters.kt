/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.search.model

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import nl.info.zac.app.shared.RestPageParameters
import nl.info.zac.search.model.ZoekParameters
import nl.info.zac.search.model.ZoekVeld
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@NoArgConstructor
@Suppress("LongParameterList")
data class RestZoekKoppelenParameters(
    @field: PositiveOrZero
    override var page: Int,

    @field: Positive
    override var rows: Int,

    // ErrorCode.ERROR_CODE_REQUIRED_SEARCH_PARAMETER_MISSING
    @field: NotBlank(message = "msg.error.search.required.parameter.missing")
    // ErrorCode.ERROR_CODE_SEARCH_PARAMETER_TOO_SHORT
    @field: Size(min = 2, message = "msg.error.search.parameter.too.short")
    var zaakIdentificator: String? = null,

    var informationObjectTypeUuid: UUID
) : RestPageParameters

fun RestZoekKoppelenParameters.toZoekParameters() = ZoekParameters(ZoekObjectType.ZAAK).apply {
    rows = this@toZoekParameters.rows
    page = this@toZoekParameters.page
    addZoekVeld(ZoekVeld.ZAAK_IDENTIFICATIE, this@toZoekParameters.zaakIdentificator!!)
}
