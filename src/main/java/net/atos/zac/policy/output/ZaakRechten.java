/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.policy.output;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;

import net.atos.zac.util.DummyInterface;

public record ZaakRechten(
                          boolean lezen,
                          boolean wijzigen,
                          boolean toekennen,
                          boolean behandelen,
                          boolean afbreken,
                          boolean heropenen,
                          boolean wijzigenZaakdata,
                          boolean wijzigenDoorlooptijd
) implements DummyInterface {

    @JsonbCreator
    public ZaakRechten(
            @JsonbProperty("lezen") final boolean lezen,
            @JsonbProperty("wijzigen") final boolean wijzigen,
            @JsonbProperty("toekennen") final boolean toekennen,
            @JsonbProperty("behandelen") final boolean behandelen,
            @JsonbProperty("afbreken") final boolean afbreken,
            @JsonbProperty("heropenen") final boolean heropenen,
            @JsonbProperty("wijzigenZaakdata") final boolean wijzigenZaakdata,
            @JsonbProperty("wijzigenDoorlooptijd") final boolean wijzigenDoorlooptijd) {
        this.lezen = lezen;
        this.wijzigen = wijzigen;
        this.toekennen = toekennen;
        this.behandelen = behandelen;
        this.afbreken = afbreken;
        this.heropenen = heropenen;
        this.wijzigenZaakdata = wijzigenZaakdata;
        this.wijzigenDoorlooptijd = wijzigenDoorlooptijd;
    }
}
