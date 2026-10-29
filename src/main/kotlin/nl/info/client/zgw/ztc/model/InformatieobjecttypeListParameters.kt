/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.ztc.model

import jakarta.ws.rs.QueryParam
import java.net.URI

class InformatieobjecttypeListParameters(
    private val catalogus: URI? = null,
    page: Int? = null
) : AbstractZtcListParameters(page) {
    /**
     * URL-referentie naar de CATALOGUS waartoe dit INFORMATIEOBJECTTYPE behoort.
     */
    @QueryParam("catalogus")
    fun getCatalogus() = catalogus
}
