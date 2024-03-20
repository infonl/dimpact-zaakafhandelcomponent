/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.ztc.model

import jakarta.ws.rs.QueryParam
import net.atos.client.zgw.shared.model.AbstractListParameters
import java.net.URI

/**
 * ListParameters for Besluittype
 */
class BesluittypeListParameters(zaaktype: URI) : AbstractListParameters() {

    /**
     * URL-referentie naar het ZAAKTYPE van ZAAKen waarin resultaten van dit RESULTAATTYPE bereikt kunnen worden.
     */
    @QueryParam("zaaktypen")
    private var zaaktypen: List<URI>? = null

    init {
        this.zaaktypen = listOf(zaaktype)
    }
}
