/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.ztc.model

import jakarta.ws.rs.QueryParam
import java.net.URI

class StatustypeListParameters(
    private val zaaktype: URI
) : AbstractZtcListParameters() {
    /**
     * URL-referentie naar het ZAAKTYPE van ZAAKen waarin STATUSsen van dit STATUSTYPE bereikt kunnen worden.
     */
    @QueryParam("zaaktype")
    fun getZaaktype() = zaaktype
}
