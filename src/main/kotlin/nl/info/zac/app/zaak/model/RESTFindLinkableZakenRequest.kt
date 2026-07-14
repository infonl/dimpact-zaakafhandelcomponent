package nl.info.zac.app.zaak.model

import jakarta.validation.constraints.NotBlank
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import java.util.UUID

class RESTFindLinkableZakenRequest {
    @PathParam("zaakUuid")
    @field:NotBlank
    lateinit var zaakUuid: UUID

    @QueryParam("zoekZaakIdentifier")
    @field:NotBlank
    lateinit var zoekZaakIdentifier: String

    @QueryParam("relationType")
    @field:NotBlank
    lateinit var relationType: RelatieType

    @QueryParam("page") @DefaultValue("0")
    var page: Int = 0

    @QueryParam("rows") @DefaultValue("10")
    var rows: Int = 10
}
