/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.brp.util

import jakarta.json.bind.Jsonb
import jakarta.json.bind.JsonbBuilder
import jakarta.json.bind.JsonbConfig
import jakarta.json.bind.serializer.DeserializationContext
import jakarta.json.bind.serializer.JsonbDeserializer
import jakarta.json.stream.JsonParser
import net.atos.client.brp.model.generated.AbstractDatum
import net.atos.client.brp.model.generated.DatumOnbekend
import net.atos.client.brp.model.generated.JaarDatum
import net.atos.client.brp.model.generated.JaarMaandDatum
import net.atos.client.brp.model.generated.VolledigeDatum
import java.lang.reflect.Type


class AbstractDatumJsonbDeserializer : JsonbDeserializer<AbstractDatum> {
    override fun deserialize(parser: JsonParser, ctx: DeserializationContext, rtType: Type): AbstractDatum {
        val jsonObject = parser.getObject()
        val type = jsonObject.getString("type")
        return when (type) {
            "Datum" -> JSONB.fromJson(jsonObject.toString(), VolledigeDatum::class.java)
            "DatumOnbekend" -> JSONB.fromJson(jsonObject.toString(), DatumOnbekend::class.java)
            "JaarDatum" -> JSONB.fromJson(jsonObject.toString(), JaarDatum::class.java)
            "JaarMaandDatum" -> JSONB.fromJson(jsonObject.toString(), JaarMaandDatum::class.java)
            else -> throw RuntimeException("Type '%s' wordt niet ondersteund".formatted(type))
        }
    }

    companion object {
        private val JSONB: Jsonb = JsonbBuilder.create(
            JsonbConfig().withPropertyVisibilityStrategy(FieldPropertyVisibilityStrategy())
        )
    }
}
