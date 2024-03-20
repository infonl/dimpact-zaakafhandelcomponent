/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.ztc.model

import jakarta.ws.rs.QueryParam
import java.net.URI

class ZaaktypeInformatieobjecttypeListParameters(
    private val zaaktype: URI
) : AbstractZTCListParameters() {
    @QueryParam("zaaktype")
    fun getZaaktype() = zaaktype
}
