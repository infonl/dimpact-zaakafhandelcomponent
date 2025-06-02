/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.zrc.model;

import java.net.URI;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;

/**
 * Een relevante andere zaak.
 *
 * @param url         URL-referentie naar de ZAAK.
 * @param aardRelatie 'Benamingen van de aard van de relaties van andere zaken tot (onderhanden) zaken.
 */
public record RelevanteZaak(URI url, AardRelatie aardRelatie) {

    /**
     * Constructor with required attributes for POST and PUT requests and GET response
     */
    @JsonbCreator
    public RelevanteZaak(
            @JsonbProperty("url") final URI url,
            @JsonbProperty("aardRelatie") final AardRelatie aardRelatie
    ) {
        this.url = url;
        this.aardRelatie = aardRelatie;
    }

    public boolean is(final URI url, final AardRelatie aardRelatie) {
        return this.aardRelatie == aardRelatie && this.url.equals(url);
    }
}
