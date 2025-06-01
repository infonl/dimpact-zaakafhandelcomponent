/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.zrc.model;

import java.net.URI;

import jakarta.json.bind.annotation.JsonbNillable;

import nl.info.client.zgw.zrc.model.generated.Zaak;

// TODO: still needed? if so, convert to Kotlin
public class HoofdzaakZaakPatch extends Zaak {

    @JsonbNillable
    private final URI hoofdzaak;

    public HoofdzaakZaakPatch(final URI hoofdzaak) {
        this.hoofdzaak = hoofdzaak;
    }

    public URI getHoofdzaak() {
        return hoofdzaak;
    }
}
