/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.ztc.model

import jakarta.ws.rs.QueryParam
import java.net.URI

class ZaaktypeListParameters(
    private val catalogus: URI
) : AbstractZtcListParameters() {
    /**
     * URL-referentie naar de CATALOGUS waartoe dit ZAAKTYPE behoort.
     */
    @QueryParam("catalogus")
    fun getCatalogus() = catalogus
}
