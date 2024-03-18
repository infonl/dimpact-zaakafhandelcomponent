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

import net.atos.client.brp.model.generated.AbstractVerblijfplaats;
import net.atos.client.brp.model.generated.Adres;
import net.atos.client.brp.model.generated.Locatie;
import net.atos.client.brp.model.generated.VerblijfplaatsBuitenland;
import net.atos.client.brp.model.generated.VerblijfplaatsOnbekend;

public class AbstractVerblijfplaatsJsonbDeserializer implements JsonbDeserializer<AbstractVerblijfplaats> {

    private static final Jsonb JSONB = JsonbBuilder.create(
            new JsonbConfig().withPropertyVisibilityStrategy(new FieldPropertyVisibilityStrategy()));

    @Override
    public AbstractVerblijfplaats deserialize(
            final JsonParser parser,
            final DeserializationContext ctx,
            final Type rtType
    ) {
        final JsonObject jsonObject = parser.getObject();
        final String type = jsonObject.getString("type");
        return switch (type) {
            case "VerblijfplaatsBuitenland" -> JSONB.fromJson(jsonObject.toString(), VerblijfplaatsBuitenland.class);
            case "Adres" -> JSONB.fromJson(jsonObject.toString(), Adres.class);
            case "VerblijfplaatsOnbekend" -> JSONB.fromJson(jsonObject.toString(), VerblijfplaatsOnbekend.class);
            case "Locatie" -> JSONB.fromJson(jsonObject.toString(), Locatie.class);
            default -> throw new RuntimeException("Type '%s' wordt niet ondersteund".formatted(type));
        };
    }
}
