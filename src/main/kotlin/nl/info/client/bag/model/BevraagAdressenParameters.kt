/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.bag.model

import jakarta.ws.rs.DefaultValue
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
    @field:DefaultValue("false")
    var exacteMatch: Boolean = false

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
    @field:DefaultValue("1")
    var page: Int = 1

    @field:QueryParam("pageSize")
    @field:DefaultValue("20")
    var pageSize: Int = 20

    @field:QueryParam("q")
    var q: String? = null

    @field:QueryParam("inclusiefEindStatus")
    @field:DefaultValue("false")
    var inclusiefEindStatus: Boolean = false
}
