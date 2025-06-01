/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.util

import jakarta.json.bind.serializer.DeserializationContext
import jakarta.json.bind.serializer.JsonbDeserializer
import jakarta.json.stream.JsonParser
import net.atos.client.zgw.shared.util.JsonbUtil.JSONB
import nl.info.client.zgw.zrc.model.generated.GeoJSONGeometry
import nl.info.client.zgw.zrc.model.generated.GeometryTypeEnum
import java.lang.reflect.Type

// TODO: at least add doc
class GeoJSONGeometryJsonbDeserializer : JsonbDeserializer<GeoJSONGeometry> {
    companion object {
        const val GEOMETRY_TYPE_JSON_FIELD_NAME = "type"
    }

    override fun deserialize(
        parser: JsonParser,
        ctx: DeserializationContext,
        rtType: Type
    ): GeoJSONGeometry? {
        if (!parser.hasNext()) {
            // workaround for WildFly 30 (?) issue
            // jakarta.ws.rs.ProcessingException: RESTEASY008200: JSON Binding deserialization
            // error: jakarta.json.bind.JsonbException: Internal error: There are no more elements available!
            // at 'parser.getObject()' call below
            return null
        }
        val jsonObject = parser.getObject()
        val geometryType = GeometryTypeEnum.fromValue(
            jsonObject.getJsonString(GEOMETRY_TYPE_JSON_FIELD_NAME).string
        )
        return when (geometryType) {
            GeometryTypeEnum.POINT -> JSONB.fromJson(jsonObject.toString(), GeoJSONGeometry::class.java)
            else -> {
                throw IllegalArgumentException(
                    "Geometry type '$geometryType' is currently not supported."
                )
            }
        }
    }
}
