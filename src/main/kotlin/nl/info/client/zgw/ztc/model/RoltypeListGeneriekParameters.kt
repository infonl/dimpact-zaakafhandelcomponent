/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.ztc.model

import jakarta.ws.rs.QueryParam
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import java.net.URI

class RoltypeListGeneriekParameters(
    private val zaaktype: URI,
    private val omschrijvingGeneriek: OmschrijvingGeneriekEnum
) : AbstractZtcListParameters() {
    /**
     * Algemeen gehanteerde omschrijving van de aard van de ROL.
     */
    @QueryParam("omschrijvingGeneriek")
    fun getOmschrijvingGeneriek() = omschrijvingGeneriek.toString()

    /**
     * URL-referentie naar het ZAAKTYPE waar deze ROLTYPEn betrokken kunnen zijn.
     */
    @QueryParam("zaaktype")
    fun getZaaktype() = zaaktype
}
