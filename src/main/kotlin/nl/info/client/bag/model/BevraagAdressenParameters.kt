/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.bag.model

import jakarta.ws.rs.QueryParam

class BevraagAdressenParameters {

    @field:QueryParam("zoekresultaatIdentificatie")
    var zoekresultaatIdentificatie: String? = null

    @field:QueryParam("postcode")
    var postcode: String? = null

    @field:QueryParam("huisnummer")
    var huisnummer: Int? = null

    @field:QueryParam("huisnummertoevoeging")
    var huisnummertoevoeging: String? = null

    @field:QueryParam("huisletter")
    var huisletter: String? = null

    @field:QueryParam("exacteMatch")
    var exacteMatch: Boolean? = null

    @field:QueryParam("adresseerbaarObjectIdentificatie")
    var adresseerbaarObjectIdentificatie: String? = null

    @field:QueryParam("woonplaatsNaam")
    var woonplaatsNaam: String? = null

    @field:QueryParam("openbareRuimteNaam")
    var openbareRuimteNaam: String? = null

    @field:QueryParam("pandIdentificatie")
    var pandIdentificatie: String? = null

    @field:QueryParam("expand")
    var expand: String? = null

    @field:QueryParam("page")
    var page: Int? = null

    @field:QueryParam("pageSize")
    var pageSize: Int? = null

    @field:QueryParam("q")
    var q: String? = null

    @field:QueryParam("inclusiefEindStatus")
    var inclusiefEindStatus: Boolean? = null
}
