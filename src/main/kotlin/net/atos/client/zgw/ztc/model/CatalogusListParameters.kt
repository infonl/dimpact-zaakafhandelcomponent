/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.ztc.model

import jakarta.ws.rs.QueryParam

class CatalogusListParameters {
    /**
     * Een afkorting waarmee wordt aangegeven voor welk domein in een CATALOGUS ZAAKTYPEn zijn uitgewerkt.
     */
    @JvmField
    @QueryParam("domein")
    var domein: String? = null

    /**
     * Multiple values may be separated by commas.
     */
    @QueryParam("domein__in")
    var domeinIn: String? = null

    /**
     * Het door een kamer toegekend uniek nummer voor de INGESCHREVEN NIET-NATUURLIJK PERSOON die de eigenaar is van een CATALOGUS.
     */
    @QueryParam("rsin")
    var rsin: String? = null

    /**
     * Multiple values may be separated by commas.
     */
    @QueryParam("rsin__in")
    var rsinIn: String? = null

    /**
     * en pagina binnen de gepagineerde set resultaten.
     */
    @QueryParam("page")
    var page: Int? = null
}
