/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.productaanvraag.util;

import jakarta.json.bind.adapter.JsonbAdapter;

import nl.info.zac.productaanvraag.model.generated.Betrokkene;

/**
 * JSON adapter for the {@link Betrokkene.IndicatieMachtiging} enum that matches on the enum's value instead
 * of the enum's name.
 */
public class IndicatieMachtigingEnumJsonAdapter implements JsonbAdapter<Betrokkene.IndicatieMachtiging, String> {
    @Override
    public String adaptToJson(Betrokkene.IndicatieMachtiging value) {
        return value.name();
    }

    @Override
    public Betrokkene.IndicatieMachtiging adaptFromJson(String s) {
        return Betrokkene.IndicatieMachtiging.fromValue(s);
    }
}
