/*
 *
 *  * SPDX-FileCopyrightText: 2025 Lifely
 *  * SPDX-License-Identifier: EUPL-1.2+
 *
 */
package nl.info.zac.app.search.model

import net.atos.zac.app.shared.RestPageParameters
import net.atos.zac.search.model.ZoekParameters
import net.atos.zac.search.model.ZoekVeld
import net.atos.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@NoArgConstructor
@Suppress("LongParameterList")
data class RestZoekKoppelenParameters(
    override var page: Int,
    override var rows: Int,
    var zaakIdentificator: String,
    var documentTypeUUID: UUID
) : RestPageParameters(page, rows)

fun RestZoekKoppelenParameters.toZoekParameters() = ZoekParameters(ZoekObjectType.ZAAK).apply {
    rows = this@toZoekParameters.rows
    page = this@toZoekParameters.page
    addZoekVeld(ZoekVeld.ZAAK_IDENTIFICATIE, this@toZoekParameters.zaakIdentificator)
}
