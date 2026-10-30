/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.ztc.model

import jakarta.ws.rs.QueryParam
import nl.info.client.zgw.shared.model.AbstractListParameters
import java.net.URI

class BesluittypeListParameters(
    zaaktype: URI? = null,

    /**
     * URL-referentie naar de CATALOGUS waartoe dit BESLUITTYPE behoort.
     */
    @field:QueryParam("catalogus")
    val catalogus: URI? = null
) : AbstractListParameters() {

    /**
     * URL-referentie naar het ZAAKTYPE van ZAAKen waarin resultaten van dit RESULTAATTYPE bereikt kunnen worden.
     */
    @field:QueryParam("zaaktypen")
    val zaaktypen: List<URI> = listOfNotNull(zaaktype)
}
