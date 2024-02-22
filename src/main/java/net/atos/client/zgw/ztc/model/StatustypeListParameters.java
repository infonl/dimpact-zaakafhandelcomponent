/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.ztc.model;

import java.net.URI;

import jakarta.ws.rs.QueryParam;

/**
 *
 */
public class StatustypeListParameters extends AbstractZTCListParameters {

    /**
     * URL-referentie naar het ZAAKTYPE van ZAAKen waarin STATUSsen van dit STATUSTYPE bereikt kunnen worden.
     */
    @QueryParam("zaaktype")
    private final URI zaaktype;

    public StatustypeListParameters(final URI zaaktype) {
        this.zaaktype = zaaktype;
    }

    public URI getZaaktype() {
        return zaaktype;
    }
}
