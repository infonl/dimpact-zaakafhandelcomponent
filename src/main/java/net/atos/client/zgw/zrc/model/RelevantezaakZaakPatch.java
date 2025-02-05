/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.zrc.model;

import java.util.List;

import jakarta.json.bind.annotation.JsonbNillable;
import jakarta.json.bind.annotation.JsonbProperty;

public class RelevantezaakZaakPatch extends Zaak {

    @JsonbNillable
    private final List<RelevanteZaak> relevanteAndereZaken;

    public RelevantezaakZaakPatch(final List<RelevanteZaak> relevanteAndereZaken) {
        this.relevanteAndereZaken = relevanteAndereZaken;
    }

    public List<RelevanteZaak> getRelevanteAndereZaken() {
        return relevanteAndereZaken;
    }
}
