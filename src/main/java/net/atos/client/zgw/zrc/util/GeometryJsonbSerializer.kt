/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.zrc.util

import jakarta.json.bind.serializer.JsonbSerializer
import jakarta.json.bind.serializer.SerializationContext
import jakarta.json.stream.JsonGenerator
import net.atos.client.zgw.shared.util.JsonbUtil.JSONB
import net.atos.client.zgw.zrc.model.Geometry
import net.atos.client.zgw.zrc.model.GeometryType
import net.atos.client.zgw.zrc.model.Point

class GeometryJsonbSerializer : JsonbSerializer<Geometry> {

    override fun serialize(
        geometry: Geometry,
        jsonGenerator: JsonGenerator,
        ctx: SerializationContext
    ) {
        // in the patch zaak endpoint of the ZRC ZGW API a null property value
        // is used to indicate that the property in question should be deleted
        if (geometry.markGeometryForDeletion) {
            jsonGenerator.writeNull()
        } else {
            when (geometry.type) {
                GeometryType.POINT -> jsonGenerator.write(JSONB.toJson(geometry, Point::class.java))
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
