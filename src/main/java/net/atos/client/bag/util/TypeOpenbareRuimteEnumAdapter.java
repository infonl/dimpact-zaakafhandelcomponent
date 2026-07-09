/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.bag.util;

import jakarta.json.bind.adapter.JsonbAdapter;

import nl.info.client.bag.model.generated.TypeOpenbareRuimte;

public class TypeOpenbareRuimteEnumAdapter implements JsonbAdapter<TypeOpenbareRuimte, String> {

    @Override
    public String adaptToJson(final TypeOpenbareRuimte typeOpenbareRuimte) {
        return typeOpenbareRuimte.toString();
    }

    @Override
    public TypeOpenbareRuimte adaptFromJson(final String json) {
        return TypeOpenbareRuimte.fromValue(json);
    }
}
