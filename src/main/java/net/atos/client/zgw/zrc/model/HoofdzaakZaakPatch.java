/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.zrc.model;

import java.net.URI;

import jakarta.json.bind.annotation.JsonbNillable;
import jakarta.json.bind.annotation.JsonbProperty;

public class HoofdzaakZaakPatch extends Zaak {

    @JsonbProperty
    @JsonbNillable
    private final URI hoofdzaak;

    public HoofdzaakZaakPatch(final URI hoofdzaak) {
        this.hoofdzaak = hoofdzaak;
    }

    public URI getHoofdzaak() {
        return hoofdzaak;
    }
}
