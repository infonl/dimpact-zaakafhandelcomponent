/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.jsonb

import jakarta.json.bind.serializer.JsonbSerializer
import jakarta.json.bind.serializer.SerializationContext
import jakarta.json.stream.JsonGenerator
import net.atos.client.zgw.shared.util.JsonbUtil
import nl.info.client.zgw.zrc.model.generated.GeoJSONGeometry
import nl.info.client.zgw.zrc.model.generated.GeometryTypeEnum

/**
 * TODO: update or delete?
 * Custom JSONB serializer for [Geometry] objects.
 * If [Geometry.markGeometryForDeletion] is set to true a null value is written
 * to indicate that the geometry field should be deleted, as per the ZGW ZRC API specification.
 */
class GeoJSONGeometryJsonbSerializer : JsonbSerializer<GeoJSONGeometry> {

    override fun serialize(
        geometry: GeoJSONGeometry,
        jsonGenerator: JsonGenerator,
        ctx: SerializationContext
    ) {
        // TODO: add support to write nulls for deletions?
        // if (geometry.markGeometryForDeletion) {
        //     jsonGenerator.writeNull()
        // } else {
        when (geometry.type) {
            GeometryTypeEnum.POINT -> jsonGenerator.write(JsonbUtil.JSONB.toJson(geometry, GeoJSONGeometry::class.java))
            else -> {
                throw IllegalArgumentException(
                    "Geometry type '${geometry.type}' is currently not supported."
                )
            }
        }
        // }
    }
}
