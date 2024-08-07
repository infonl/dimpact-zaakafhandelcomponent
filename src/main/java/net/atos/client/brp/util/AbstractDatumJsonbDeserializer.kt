/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.brp.util;


import java.lang.reflect.Type;

import jakarta.json.JsonObject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.stream.JsonParser;

import net.atos.client.brp.model.generated.AbstractDatum;
import net.atos.client.brp.model.generated.DatumOnbekend;
import net.atos.client.brp.model.generated.JaarDatum;
import net.atos.client.brp.model.generated.JaarMaandDatum;
import net.atos.client.brp.model.generated.VolledigeDatum;

public class AbstractDatumJsonbDeserializer implements JsonbDeserializer<AbstractDatum> {

    private static final Jsonb JSONB = JsonbBuilder.create(
            new JsonbConfig().withPropertyVisibilityStrategy(new FieldPropertyVisibilityStrategy()));

    @Override
    public AbstractDatum deserialize(final JsonParser parser, final DeserializationContext ctx, final Type rtType) {
        final JsonObject jsonObject = parser.getObject();
        final String type = jsonObject.getString("type");
        return switch (type) {
            case "Datum" -> JSONB.fromJson(jsonObject.toString(), VolledigeDatum.class);
            case "DatumOnbekend" -> JSONB.fromJson(jsonObject.toString(), DatumOnbekend.class);
            case "JaarDatum" -> JSONB.fromJson(jsonObject.toString(), JaarDatum.class);
            case "JaarMaandDatum" -> JSONB.fromJson(jsonObject.toString(), JaarMaandDatum.class);
            default -> throw new RuntimeException("Type '%s' wordt niet ondersteund".formatted(type));
        };
    }
}
