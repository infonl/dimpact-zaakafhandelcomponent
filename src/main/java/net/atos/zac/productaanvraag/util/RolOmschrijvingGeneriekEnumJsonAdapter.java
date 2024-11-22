/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.productaanvraag.util;

import jakarta.json.bind.adapter.JsonbAdapter;

import net.atos.zac.productaanvraag.model.generated.Betrokkene;

/**
 * JSON adapter for the {@link Betrokkene.RolOmschrijvingGeneriek} enum that matches on the enum's value instead
 * of the enum's name.
 */
public class RolOmschrijvingGeneriekEnumJsonAdapter implements JsonbAdapter<Betrokkene.RolOmschrijvingGeneriek, String> {
    @Override
    public String adaptToJson(Betrokkene.RolOmschrijvingGeneriek value) {
        return value.name();
    }

    @Override
    public Betrokkene.RolOmschrijvingGeneriek adaptFromJson(String s) {
        return Betrokkene.RolOmschrijvingGeneriek.fromValue(s);
    }
}
