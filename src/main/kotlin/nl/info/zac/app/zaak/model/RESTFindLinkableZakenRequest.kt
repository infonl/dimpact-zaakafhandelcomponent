/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.zaak.model

import jakarta.validation.constraints.NotBlank
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import java.util.UUID

class RESTFindLinkableZakenRequest {
    @PathParam("zaakUuid")
    @field:jakarta.validation.constraints.NotNull
    lateinit var zaakUuid: UUID

    @QueryParam("zoekZaakIdentifier")
    @field:NotBlank
    lateinit var zoekZaakIdentifier: String

    @QueryParam("relationType")
    @field:jakarta.validation.constraints.NotNull
    lateinit var relationType: RelatieType

    @QueryParam("page") @DefaultValue("0")
    @field:jakarta.validation.constraints.PositiveOrZero
    var page: Int = 0

    @QueryParam("rows") @DefaultValue("10")
    @field:jakarta.validation.constraints.Positive
    var rows: Int = 10
}
