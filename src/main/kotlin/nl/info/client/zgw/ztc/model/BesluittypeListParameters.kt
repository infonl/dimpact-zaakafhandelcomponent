/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.ztc.model

import jakarta.ws.rs.QueryParam
import net.atos.client.zgw.shared.model.AbstractListParameters
import java.net.URI

class BesluittypeListParameters(zaaktype: URI) : AbstractListParameters() {

    /**
     * URL-referentie naar het ZAAKTYPE van ZAAKen waarin resultaten van dit RESULTAATTYPE bereikt kunnen worden.
     */
    @field:QueryParam("zaaktypen")
    val zaaktypen: List<URI> = listOf(zaaktype)
}
