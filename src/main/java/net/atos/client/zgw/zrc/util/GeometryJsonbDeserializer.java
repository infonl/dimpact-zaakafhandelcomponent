/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.zgw.zrc.util;

import static net.atos.client.zgw.shared.util.JsonbUtil.JSONB;
import static net.atos.client.zgw.zrc.model.Geometry.GEOMETRY_TYPE_NAAM;

import java.lang.reflect.Type;

import jakarta.json.JsonObject;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.stream.JsonParser;

import net.atos.client.zgw.zrc.model.Geometry;
import net.atos.client.zgw.zrc.model.GeometryCollection;
import net.atos.client.zgw.zrc.model.GeometryType;
import net.atos.client.zgw.zrc.model.Point;
import net.atos.client.zgw.zrc.model.Polygon;

public class GeometryJsonbDeserializer implements JsonbDeserializer<Geometry> {

    @Override
    public Geometry deserialize(
            final JsonParser parser,
            final DeserializationContext ctx,
            final Type rtType
    ) {
        if (!parser.hasNext()) {
            // workaround for WildFly 30 (?) issue
            // jakarta.ws.rs.ProcessingException: RESTEASY008200: JSON Binding deserialization
            // error: jakarta.json.bind.JsonbException: Internal error: There are no more elements available!
            // at 'parser.getObject()' call below
            return null;
        }
        final JsonObject jsonObject = parser.getObject();
        final GeometryType geometryType = GeometryType.fromValue(
                jsonObject.getJsonString(GEOMETRY_TYPE_NAAM).getString());

        return switch (geometryType) {
            case POINT -> JSONB.fromJson(jsonObject.toString(), Point.class);
            case POLYGON -> JSONB.fromJson(jsonObject.toString(), Polygon.class);
            case GEOMETRYCOLLECTION -> JSONB.fromJson(jsonObject.toString(), GeometryCollection.class);
        };
    }
}
