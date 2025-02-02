/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.zrc.model;

import jakarta.json.bind.annotation.JsonbNillable;

/**
 * Zaak locatie patch data
 */
public class LocatieZaakPatch extends Zaak {
    private final Geometry zaakgeometrie;

    public LocatieZaakPatch(final Geometry zaakgeometrie) {
        this.zaakgeometrie = zaakgeometrie;
    }

    @JsonbNillable
    public Geometry getZaakgeometrie() {
        return zaakgeometrie;
    }
}
