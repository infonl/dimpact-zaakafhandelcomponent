/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.bag.util;

import jakarta.json.bind.adapter.JsonbAdapter;

import nl.info.client.bag.model.generated.Indicatie;

public class IndicatieEnumAdapter implements JsonbAdapter<Indicatie, String> {

    @Override
    public String adaptToJson(final Indicatie indicatie) {
        return indicatie.toString();
    }

    @Override
    public Indicatie adaptFromJson(final String json) {
        return Indicatie.fromValue(json);
    }
}
