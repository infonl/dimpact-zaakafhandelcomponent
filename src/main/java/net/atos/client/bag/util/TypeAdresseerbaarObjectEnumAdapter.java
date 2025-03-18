/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.bag.util;

import jakarta.json.bind.adapter.JsonbAdapter;

import nl.info.client.bag.model.generated.TypeAdresseerbaarObject;

public class TypeAdresseerbaarObjectEnumAdapter implements JsonbAdapter<TypeAdresseerbaarObject, String> {

    @Override
    public String adaptToJson(final TypeAdresseerbaarObject typeAdresseerbaarObject) {
        return typeAdresseerbaarObject.toString();
    }

    @Override
    public TypeAdresseerbaarObject adaptFromJson(final String json) {
        return TypeAdresseerbaarObject.fromValue(json);
    }
}
