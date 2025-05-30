/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.util

import jakarta.json.bind.serializer.DeserializationContext
import jakarta.json.bind.serializer.JsonbDeserializer
import jakarta.json.stream.JsonParser
import net.atos.client.zgw.shared.util.JsonbUtil.JSONB
import net.atos.client.zgw.zrc.model.Geometry
import net.atos.client.zgw.zrc.model.GeometryCollection
import net.atos.client.zgw.zrc.model.GeometryType
import net.atos.client.zgw.zrc.model.Point
import net.atos.client.zgw.zrc.model.Polygon
import java.lang.reflect.Type

// TODO: refactor this. or can it be removed even?
class GeometryJsonbDeserializer : JsonbDeserializer<Geometry> {
    override fun deserialize(
        parser: JsonParser,
        ctx: DeserializationContext,
        rtType: Type
    ): Geometry? {
        if (!parser.hasNext()) {
            // workaround for WildFly 30 (?) issue
            // jakarta.ws.rs.ProcessingException: RESTEASY008200: JSON Binding deserialization
            // error: jakarta.json.bind.JsonbException: Internal error: There are no more elements available!
            // at 'parser.getObject()' call below
            return null
        }
        val jsonObject = parser.getObject()
        val geometryType = GeometryType.fromValue(
            jsonObject.getJsonString(Geometry.GEOMETRY_TYPE_NAAM).string
        )

        return when (geometryType) {
            GeometryType.POINT -> JSONB.fromJson(jsonObject.toString(), Point::class.java)
            GeometryType.POLYGON -> JSONB.fromJson(jsonObject.toString(), Polygon::class.java)
            GeometryType.GEOMETRYCOLLECTION -> JSONB.fromJson(
                jsonObject.toString(),
                GeometryCollection::class.java
            )
        }
    }
}
