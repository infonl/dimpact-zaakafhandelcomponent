/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.ztc.model;

import java.net.URI;

import jakarta.ws.rs.QueryParam;

import net.atos.client.zgw.ztc.model.generated.RolType;

/**
 *
 */
public class RoltypeListParameters extends AbstractZTCListParameters {

    /*
     * URL-referentie naar het ZAAKTYPE waar deze ROLTYPEn betrokken kunnen zijn.
     */
    @QueryParam("zaaktype")
    private URI zaaktype;

    /*
     * Algemeen gehanteerde omschrijving van de aard van de ROL.
     */
    private RolType.OmschrijvingGeneriekEnum omschrijvingGeneriek;

    public RoltypeListParameters(final URI zaaktype) {
        this.zaaktype = zaaktype;
    }

    public RoltypeListParameters(final RolType.OmschrijvingGeneriekEnum omschrijvingGeneriek) {
        this.omschrijvingGeneriek = omschrijvingGeneriek;
    }

    public RoltypeListParameters(final URI zaaktype, final RolType.OmschrijvingGeneriekEnum omschrijvingGeneriek) {
        this.zaaktype = zaaktype;
        this.omschrijvingGeneriek = omschrijvingGeneriek;
    }

    public URI getZaaktype() {
        return zaaktype;
    }

    @QueryParam("omschrijvingGeneriek")
    public String getOmschrijvingGeneriek() {
        return omschrijvingGeneriek == null ? null : omschrijvingGeneriek.value();
    }
}
