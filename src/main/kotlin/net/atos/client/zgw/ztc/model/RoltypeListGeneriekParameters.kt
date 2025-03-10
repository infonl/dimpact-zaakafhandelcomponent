/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.ztc.model

import jakarta.ws.rs.QueryParam
import net.atos.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import java.net.URI

class RoltypeListGeneriekParameters(
    private val zaaktype: URI,
    private val omschrijvingGeneriek: OmschrijvingGeneriekEnum? = null
) : AbstractZtcListParameters() {
    /**
     * Algemeen gehanteerde omschrijving van de aard van de ROL.
     */
    @QueryParam("omschrijvingGeneriek")
    fun getOmschrijvingGeneriek() = omschrijvingGeneriek?.toString()

    /**
     * URL-referentie naar het ZAAKTYPE waar deze ROLTYPEn betrokken kunnen zijn.
     */
    @QueryParam("zaaktype")
    fun getZaaktype() = zaaktype
}
