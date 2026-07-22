/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.zaak.model

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import nl.info.zac.app.search.model.RestDatumRange
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
class RestFindLinkableZakenRequest {

    @field:Size(min = 2)
    var zoekZaakIdentifier: String? = null

    @field:NotNull
    lateinit var relationType: RelatieType

    @field:Size(min = 2)
    var zoekZaakOmschrijving: String? = null

    var startdatum: RestDatumRange? = null

    var einddatum: RestDatumRange? = null

    var zoekZaakTypeOmschrijving: String? = null

    @field:PositiveOrZero
    var page: Int = 0

    @field:Positive
    var rows: Int = 10
}
