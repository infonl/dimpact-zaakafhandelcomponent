/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.jsonb

import jakarta.json.bind.serializer.JsonbSerializer
import jakarta.json.bind.serializer.SerializationContext
import jakarta.json.stream.JsonGenerator
import net.atos.client.zgw.shared.util.JsonbUtil
import nl.info.client.zgw.zrc.model.GeoJSONGeometryWithDeletionSupport
import nl.info.client.zgw.zrc.model.generated.GeoJSONGeometry
import nl.info.client.zgw.zrc.model.generated.GeometryTypeEnum

/**
 * Custom JSONB serializer for [GeoJSONGeometryWithDeletionSupport] objects.
 * If [GeoJSONGeometryWithDeletionSupport.markGeometryForDeletion] is set to true a `null` value is written
 * to indicate that the geometry field should be deleted, as per the ZGW ZRC API specification.
 */
class GeoJSONGeometryWithDeletionSupportJsonbSerializer : JsonbSerializer<GeoJSONGeometryWithDeletionSupport> {

    override fun serialize(
        geometryWithDeletionSupport: GeoJSONGeometryWithDeletionSupport,
        jsonGenerator: JsonGenerator,
        ctx: SerializationContext
    ) {
        if (geometryWithDeletionSupport.markGeometryForDeletion) {
            jsonGenerator.writeNull()
        } else {
            when (geometryWithDeletionSupport.type) {
                GeometryTypeEnum.POINT -> jsonGenerator.write(
                    JsonbUtil.JSONB.toJson(geometryWithDeletionSupport, GeoJSONGeometry::class.java)
                )
                else -> {
                    throw IllegalArgumentException(
                        "Geometry type '${geometryWithDeletionSupport.type}' is currently not supported."
                    )
                }
            }
        }
    }
}
