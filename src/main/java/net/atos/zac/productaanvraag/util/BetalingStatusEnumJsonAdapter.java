/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.productaanvraag.util;

import jakarta.json.bind.adapter.JsonbAdapter;

import nl.info.zac.productaanvraag.model.generated.Betaling;

/**
 * JSON adapter for the {@link Betaling.Status} enum that matches on the enum's value instead
 * of the enum's name.
 */
public class BetalingStatusEnumJsonAdapter implements JsonbAdapter<Betaling.Status, String> {
    @Override
    public String adaptToJson(Betaling.Status value) {
        return value.name();
    }

    @Override
    public Betaling.Status adaptFromJson(String s) {
        return Betaling.Status.fromValue(s);
    }
}
