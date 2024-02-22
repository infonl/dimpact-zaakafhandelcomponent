/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.policy.output;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;

import net.atos.zac.util.DummyInterface;

public record OverigeRechten(boolean startenZaak, boolean beheren, boolean zoeken)
    implements DummyInterface {

  @JsonbCreator
  public OverigeRechten(
      @JsonbProperty("starten_zaak") final boolean startenZaak,
      @JsonbProperty("beheren") final boolean beheren,
      @JsonbProperty("zoeken") final boolean zoeken) {
    this.startenZaak = startenZaak;
    this.beheren = beheren;
    this.zoeken = zoeken;
  }
}
