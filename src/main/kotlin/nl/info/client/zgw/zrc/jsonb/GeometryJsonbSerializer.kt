/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.jsonb

import jakarta.json.bind.serializer.JsonbSerializer
import jakarta.json.bind.serializer.SerializationContext
import jakarta.json.stream.JsonGenerator
import net.atos.client.zgw.shared.util.JsonbUtil
import net.atos.client.zgw.zrc.model.Geometry
import net.atos.client.zgw.zrc.model.GeometryType
import net.atos.client.zgw.zrc.model.Point

/**
 * Custom JSONB serializer for [Geometry] objects.
 * If [Geometry.markGeometryForDeletion] is set to true a null value is written
 * to indicate that the geometry field should be deleted, as per the ZGW ZRC API specification.
 */
class GeometryJsonbSerializer : JsonbSerializer<Geometry> {

    override fun serialize(
        geometry: Geometry,
        jsonGenerator: JsonGenerator,
        ctx: SerializationContext
    ) {
        if (geometry.markGeometryForDeletion) {
            jsonGenerator.writeNull()
        } else {
            when (geometry.type) {
                GeometryType.POINT -> jsonGenerator.write(JsonbUtil.JSONB.toJson(geometry, Point::class.java))
                GeometryType.POLYGON -> throw IllegalArgumentException(
                    "Polygon serialization is not implemented"
                )
                GeometryType.GEOMETRYCOLLECTION -> throw IllegalArgumentException(
                    "GeometryCollection serialization is not implemented"
                )
            }
        }
    }
}
