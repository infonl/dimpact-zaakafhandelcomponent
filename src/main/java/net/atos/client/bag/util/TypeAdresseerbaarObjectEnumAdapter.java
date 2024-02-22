/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.bag.util;

import jakarta.json.bind.adapter.JsonbAdapter;

import net.atos.client.bag.model.TypeAdresseerbaarObject;

public class TypeAdresseerbaarObjectEnumAdapter
        implements JsonbAdapter<TypeAdresseerbaarObject, String> {

    @Override
    public String adaptToJson(final TypeAdresseerbaarObject typeAdresseerbaarObject) {
        return typeAdresseerbaarObject.toString();
    }

    @Override
    public TypeAdresseerbaarObject adaptFromJson(final String json) {
        return TypeAdresseerbaarObject.fromValue(json);
    }
}
