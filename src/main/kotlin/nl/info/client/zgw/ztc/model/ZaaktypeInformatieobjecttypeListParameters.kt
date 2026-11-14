/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.ztc.model

import jakarta.ws.rs.QueryParam
import java.net.URI

class ZaaktypeInformatieobjecttypeListParameters(
    private val zaaktype: URI
) : AbstractZtcListParameters() {
    @QueryParam("zaaktype")
    fun getZaaktype() = zaaktype
}
