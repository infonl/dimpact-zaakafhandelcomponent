/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.kvk.model

import jakarta.ws.rs.QueryParam
import java.math.BigDecimal

class KvkZoekenParameters {
    @QueryParam("kvkNummer")
    var kvkNummer: String? = null

    @JvmField
    @QueryParam("rsin")
    var rsin: String? = null

    @JvmField
    @QueryParam("vestigingsnummer")
    var vestigingsnummer: String? = null

    @QueryParam("naam")
    var naam: String? = null

    @QueryParam("straatnaam")
    var straatnaam: String? = null

    @QueryParam("plaats")
    var plaats: String? = null

    @QueryParam("postcode")
    var postcode: String? = null

    @QueryParam("huisnummer")
    var huisnummer: String? = null

    @JvmField
    @QueryParam("type")
    var type: String? = null

    @QueryParam("InclusiefInactieveRegistraties")
    var inclusiefInactieveRegistraties: Boolean? = null

    @QueryParam("pagina")
    var pagina: BigDecimal? = null

    @QueryParam("aantal")
    var resultatenPerPagina: BigDecimal? = null
}
