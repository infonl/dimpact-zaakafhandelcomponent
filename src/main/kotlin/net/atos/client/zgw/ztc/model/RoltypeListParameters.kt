/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.ztc.model

import jakarta.ws.rs.QueryParam
import net.atos.client.zgw.ztc.model.generated.RolType.OmschrijvingGeneriekEnum
import java.net.URI

/**
 *
 */
class RoltypeListParameters : AbstractZTCListParameters {
    /*
     * URL-referentie naar het ZAAKTYPE waar deze ROLTYPEn betrokken kunnen zijn.
     */
    @QueryParam("zaaktype")
    var zaaktype: URI? = null
        private set

    /*
     * Algemeen gehanteerde omschrijving van de aard van de ROL.
     */
    private var omschrijvingGeneriek: OmschrijvingGeneriekEnum? = null

    constructor(zaaktype: URI?) {
        this.zaaktype = zaaktype
    }

    constructor(omschrijvingGeneriek: OmschrijvingGeneriekEnum?) {
        this.omschrijvingGeneriek = omschrijvingGeneriek
    }

    constructor(zaaktype: URI?, omschrijvingGeneriek: OmschrijvingGeneriekEnum?) {
        this.zaaktype = zaaktype
        this.omschrijvingGeneriek = omschrijvingGeneriek
    }

    @QueryParam("omschrijvingGeneriek")
    fun getOmschrijvingGeneriek(): String? {
        return if (omschrijvingGeneriek == null) null else omschrijvingGeneriek!!.value()
    }
}
