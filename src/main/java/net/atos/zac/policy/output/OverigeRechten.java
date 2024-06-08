/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.policy.output;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;

import net.atos.zac.util.SerializableByYasson;

public record OverigeRechten(boolean startenZaak,
                             boolean beheren,
                             boolean zoeken,
                             boolean sjabloonToewijzing) implements SerializableByYasson {

    @JsonbCreator
    public OverigeRechten(
            @JsonbProperty("starten_zaak") final boolean startenZaak,
            @JsonbProperty("beheren") final boolean beheren,
            @JsonbProperty("zoeken") final boolean zoeken,
            @JsonbProperty("sjabloon_toewijzing") final boolean sjabloonToewijzing
    ) {
        this.startenZaak = startenZaak;
        this.beheren = beheren;
        this.zoeken = zoeken;
        this.sjabloonToewijzing = sjabloonToewijzing;
    }
}
