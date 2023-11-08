/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.drc.model;

import java.time.LocalDate;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;

/**
 * Aanduiding van de rechtskracht van een informatieobject.
 * Mag niet van een waarde zijn voorzien als de `status` de waarde 'in bewerking' of 'ter vaststelling' heeft.
 */
public class Ondertekening {

    /**
     * Aanduiding van de wijze van ondertekening van het INFORMATIEOBJECT
     */
    private final OndertekeningSoort soort;

    /**
     * De datum waarop de ondertekening van het INFORMATIEOBJECT heeft plaatsgevonden.
     */
    private final LocalDate datum;

    /**
     * Constructor with required attributes for POST and PUT request and for GET Response
     */
    @JsonbCreator
    public Ondertekening(@JsonbProperty("soort") final OndertekeningSoort soort,
            @JsonbProperty("datum") final LocalDate datum) {
        this.soort = soort;
        this.datum = datum;
    }

    public OndertekeningSoort getSoort() {
        return soort;
    }

    public LocalDate getDatum() {
        return datum;
    }
}
