/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.jsonb

import jakarta.json.bind.serializer.JsonbSerializer
import jakarta.json.bind.serializer.SerializationContext
import jakarta.json.stream.JsonGenerator
import nl.info.client.zgw.zrc.model.DeleteGeoJsonGeometry

/**
 * Custom JSONB serializer for [DeleteGeoJsonGeometry] objects.
 * Writes a `null` value to indicate that the geometry field should be deleted, as per the ZGW ZRC API specification.
 */
class DeleteGeoJsonGeometryJsonbSerializer : JsonbSerializer<DeleteGeoJsonGeometry> {

    override fun serialize(
        deleteGeoJsonGeometry: DeleteGeoJsonGeometry,
        jsonGenerator: JsonGenerator,
        ctx: SerializationContext
    ) {
        jsonGenerator.writeNull()
    }
}
