/*
 *
 *  * SPDX-FileCopyrightText: 2025 Lifely
 *  * SPDX-License-Identifier: EUPL-1.2+
 *
 */
package nl.info.zac.app.search.model

import net.atos.zac.app.shared.RestPageParameters
import net.atos.zac.search.model.FilterParameters
import net.atos.zac.search.model.FilterVeld
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
    var documentUUID: UUID
) : RestPageParameters(page, rows)

fun RestZoekKoppelenParameters.toRestZoekParameters() =
    RestZoekParameters(
        page = page,
        rows = rows,
        zoeken = mapOf(ZoekVeld.ZAAK_IDENTIFICATIE.veld to zaakIdentificator),
        filters = mapOf(FilterVeld.ZAAK_ZAAKTYPE_UUID to FilterParameters(listOf("*"), false)),
        type = ZoekObjectType.ZAAK
    )
