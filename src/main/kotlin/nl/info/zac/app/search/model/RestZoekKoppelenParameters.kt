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
import nl.info.zac.search.model.ZoekParameters
import nl.info.zac.search.model.ZoekVeld
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@NoArgConstructor
@Suppress("LongParameterList")
data class RestZoekKoppelenParameters(
    @field:PositiveOrZero
    override var page: Int,

    @field: Positive
    override var rows: Int,

    var zaakIdentificator: String,
    var informationObjectTypeUuid: UUID
) : RestPageParameters(page, rows)

fun RestZoekKoppelenParameters.toZoekParameters() = ZoekParameters(ZoekObjectType.ZAAK).apply {
    rows = this@toZoekParameters.rows
    page = this@toZoekParameters.page
    addZoekVeld(ZoekVeld.ZAAK_IDENTIFICATIE, this@toZoekParameters.zaakIdentificator)
}
