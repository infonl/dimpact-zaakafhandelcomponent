/*
 * SPDX-FileCopyrightText: 2023 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.zrc.model;

import java.util.List;

import nl.info.client.zgw.zrc.model.generated.RelevanteZaak;
import nl.info.client.zgw.zrc.model.generated.Zaak;

// TODO: still needed? if so, convert to Kotlin
public class RelevantezaakZaakPatch extends Zaak {

    public RelevantezaakZaakPatch(final List<RelevanteZaak> relevanteAndereZaken) {
        this.relevanteAndereZaken = relevanteAndereZaken;
    }
}
