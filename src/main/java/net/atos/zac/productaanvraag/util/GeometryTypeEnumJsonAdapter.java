/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.productaanvraag.util;

import jakarta.json.bind.adapter.JsonbAdapter;

import net.atos.zac.productaanvraag.model.generated.Geometry;

/**
 * JSON adapter for the {@link net.atos.zac.productaanvraag.model.generated.Geometry.Type} enum that matches on the enum's value instead
 * of the enum's name.
 */
public class GeometryTypeEnumJsonAdapter implements JsonbAdapter<Geometry.Type, String> {
    @Override
    public String adaptToJson(Geometry.Type value) {
        return value.name();
    }

    @Override
    public Geometry.Type adaptFromJson(String s) {
        return Geometry.Type.fromValue(s);
    }
}
