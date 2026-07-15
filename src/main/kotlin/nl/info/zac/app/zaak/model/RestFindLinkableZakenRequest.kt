/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.zaak.model

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import java.util.UUID

class RestFindLinkableZakenRequest {
    @PathParam("zaakUuid")
    @field:NotNull
    lateinit var zaakUuid: UUID

    @QueryParam("zoekZaakIdentifier")
    var zoekZaakIdentifier: String? = null

    @QueryParam("relationType")
    @field:NotNull
    lateinit var relationType: RelatieType

    @QueryParam(value = "zoekZaakOmschrijving")
    var zoekZaakOmschrijving: String? = null

    @QueryParam("page") @DefaultValue("0")
    @field:PositiveOrZero
    var page: Int = 0

    @QueryParam("rows") @DefaultValue("10")
    @field:Positive
    var rows: Int = 10
}
