/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.kvk.model

import jakarta.ws.rs.QueryParam
import java.math.BigDecimal

// Would like to use a data class here but unfortunately cannot get that to work with the JAX-RS @BeanParam annotation
// which we use in the KVK clients.
class KvkSearchParameters {
    @QueryParam("huisnummer")
    var huisnummer: String? = null

    @QueryParam("InclusiefInactieveRegistraties")
    var inclusiefInactieveRegistraties: Boolean? = null

    @QueryParam("kvkNummer")
    var kvkNummer: String? = null

    @QueryParam("naam")
    var naam: String? = null

    @QueryParam("pagina")
    var page: BigDecimal? = null

    @QueryParam("plaats")
    var plaats: String? = null

    @QueryParam("postcode")
    var postcode: String? = null

    @QueryParam("aantal")
    var resultsPerPage: BigDecimal? = null

    @QueryParam("rsin")
    var rsin: String? = null

    @QueryParam("straatnaam")
    var straatnaam: String? = null

    @QueryParam("type")
    var type: String? = null

    @QueryParam("vestigingsnummer")
    var vestigingsnummer: String? = null
}
