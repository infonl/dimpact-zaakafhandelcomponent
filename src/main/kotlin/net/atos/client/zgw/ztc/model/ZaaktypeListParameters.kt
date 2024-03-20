/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.ztc.model

import jakarta.ws.rs.QueryParam
import java.net.URI

/**
 *
 */
class ZaaktypeListParameters : AbstractZTCListParameters {
    /**
     * URL-referentie naar de CATALOGUS waartoe dit ZAAKTYPE behoort.
     */
    @QueryParam("catalogus")
    var catalogus: URI? = null

    /**
     * Unieke identificatie van het ZAAKTYPE binnen de CATALOGUS waarin het ZAAKTYPE voorkomt.
     */
    @QueryParam("identificatie")
    var identificatie: String? = null

    /**
     * Multiple values may be separated by commas.
     */
    @QueryParam("trefwoorden")
    var trefwoorden: String? = null

    constructor()

    constructor(catalogus: URI?) {
        this.catalogus = catalogus
    }
}
