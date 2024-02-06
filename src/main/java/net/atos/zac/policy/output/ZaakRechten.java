/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.policy.output;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;
import net.atos.zac.util.DummyInterface;

public class ZaakRechten implements DummyInterface {

    private final boolean lezen;

    private final boolean wijzigen;

    private final boolean toekennen;

    private final boolean behandelen;

    private final boolean afbreken;

    private final boolean heropenen;

    private final boolean wijzigenZaakdata;

    private final boolean wijzigenDoorlooptijd;

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

    public boolean getLezen() {
        return lezen;
    }

    public boolean getWijzigen() { return wijzigen; }

    public boolean getToekennen() {
        return toekennen;
    }

    public boolean getBehandelen() {
        return behandelen;
    }

    public boolean getAfbreken() {
        return afbreken;
    }

    public boolean getHeropenen() {
        return heropenen;
    }

    public boolean getWijzigenZaakdata() {
        return wijzigenZaakdata;
    }

    public boolean getWijzigenDoorlooptijd() {
        return wijzigenDoorlooptijd;
    }
}
