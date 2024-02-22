/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.zrc.model;

import java.util.List;

import jakarta.json.bind.annotation.JsonbProperty;

public class RelevantezaakZaakPatch extends Zaak {

    @JsonbProperty(nillable = true)
    private final List<RelevanteZaak> relevanteAndereZaken;

    public RelevantezaakZaakPatch(final List<RelevanteZaak> relevanteAndereZaken) {
        this.relevanteAndereZaken = relevanteAndereZaken;
    }

    public List<RelevanteZaak> getRelevanteAndereZaken() {
        return relevanteAndereZaken;
    }
}
