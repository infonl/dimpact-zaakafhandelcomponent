/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.policy.output;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;
import net.atos.zac.util.DummyInterface;

public class TaakRechten implements DummyInterface {

    private final boolean lezen;

    private final boolean wijzigen;

    private final boolean toekennen;

    @JsonbCreator
    public TaakRechten(
            @JsonbProperty("lezen") final boolean lezen,
            @JsonbProperty("wijzigen") final boolean wijzigen,
            @JsonbProperty("toekennen") final boolean toekennen) {
        this.lezen = lezen;
        this.wijzigen = wijzigen;
        this.toekennen = toekennen;
    }

    public boolean getLezen() {
        return lezen;
    }

    public boolean getWijzigen() {
        return wijzigen;
    }

    public boolean getToekennen() {
        return toekennen;
    }
}
