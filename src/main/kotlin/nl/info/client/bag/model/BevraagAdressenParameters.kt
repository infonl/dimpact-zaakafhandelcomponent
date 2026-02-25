/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.bag.model

import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.QueryParam

class BevraagAdressenParameters {

    @QueryParam("zoekresultaatIdentificatie")
    var zoekresultaatIdentificatie: String? = null

    @QueryParam("postcode")
    var postcode: String? = null

    @QueryParam("huisnummer")
    var huisnummer: Int? = null

    @QueryParam("huisnummertoevoeging")
    var huisnummertoevoeging: String? = null

    @QueryParam("huisletter")
    var huisletter: String? = null

    @QueryParam("exacteMatch")
    @DefaultValue("false")
    var exacteMatch: Boolean = false

    @QueryParam("adresseerbaarObjectIdentificatie")
    var adresseerbaarObjectIdentificatie: String? = null

    @QueryParam("woonplaatsNaam")
    var woonplaatsNaam: String? = null

    @QueryParam("openbareRuimteNaam")
    var openbareRuimteNaam: String? = null

    @QueryParam("pandIdentificatie")
    var pandIdentificatie: String? = null

    @QueryParam("expand")
    var expand: String? = null

    @QueryParam("page")
    @DefaultValue("1")
    var page: Int = 1

    @QueryParam("pageSize")
    @DefaultValue("20")
    var pageSize: Int = 20

    @QueryParam("q")
    var q: String? = null

    @QueryParam("inclusiefEindStatus")
    @DefaultValue("false")
    var inclusiefEindStatus: Boolean = false
}
