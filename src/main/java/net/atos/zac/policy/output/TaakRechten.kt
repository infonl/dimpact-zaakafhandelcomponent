/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.policy.output;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;

import net.atos.zac.util.SerializableByYasson;

public record TaakRechten(
                          boolean lezen,
                          boolean wijzigen,
                          boolean toekennen,
                          boolean creeerenDocument,
                          boolean toevoegenDocument
) implements SerializableByYasson {

    @JsonbCreator
    public TaakRechten(
            @JsonbProperty("lezen") final boolean lezen,
            @JsonbProperty("wijzigen") final boolean wijzigen,
            @JsonbProperty("toekennen") final boolean toekennen,
            @JsonbProperty("creeeren_document") boolean creeerenDocument,
            @JsonbProperty("toevoegen_document") final boolean toevoegenDocument
    ) {
        this.lezen = lezen;
        this.wijzigen = wijzigen;
        this.toekennen = toekennen;
        this.creeerenDocument = creeerenDocument;
        this.toevoegenDocument = toevoegenDocument;
    }
}
