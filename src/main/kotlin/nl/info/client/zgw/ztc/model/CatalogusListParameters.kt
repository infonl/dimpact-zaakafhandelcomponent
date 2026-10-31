/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.ztc.model

import jakarta.ws.rs.QueryParam

class CatalogusListParameters {
    /**
     * Een afkorting waarmee wordt aangegeven voor welk domein in een CATALOGUS ZAAKTYPEn zijn uitgewerkt.
     */
    @field:QueryParam("domein")
    var domein: String? = null

    /**
     * Multiple values may be separated by commas.
     */
    @field:QueryParam("domein__in")
    var domeinIn: String? = null

    /**
     * Het door een kamer toegekend uniek nummer voor de INGESCHREVEN NIET-NATUURLIJK PERSOON die de eigenaar is van een CATALOGUS.
     */
    @field:QueryParam("rsin")
    var rsin: String? = null

    /**
     * Multiple values may be separated by commas.
     */
    @field:QueryParam("rsin__in")
    var rsinIn: String? = null

    /**
     * en pagina binnen de gepagineerde set resultaten.
     */
    @field:QueryParam("page")
    var page: Int? = null
}
