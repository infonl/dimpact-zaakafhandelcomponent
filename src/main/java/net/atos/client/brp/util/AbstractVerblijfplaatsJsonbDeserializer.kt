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
import net.atos.client.brp.model.generated.AbstractVerblijfplaats
import net.atos.client.brp.model.generated.Adres
import net.atos.client.brp.model.generated.Locatie
import net.atos.client.brp.model.generated.VerblijfplaatsBuitenland
import net.atos.client.brp.model.generated.VerblijfplaatsOnbekend
import java.lang.reflect.Type

class AbstractVerblijfplaatsJsonbDeserializer : JsonbDeserializer<AbstractVerblijfplaats> {
    override fun deserialize(
        parser: JsonParser,
        ctx: DeserializationContext,
        rtType: Type
    ): AbstractVerblijfplaats {
        val jsonObject = parser.getObject()
        val type = jsonObject.getString("type")
        return when (type) {
            "VerblijfplaatsBuitenland" -> JSONB.fromJson(jsonObject.toString(), VerblijfplaatsBuitenland::class.java)
            "Adres" -> JSONB.fromJson(jsonObject.toString(), Adres::class.java)
            "VerblijfplaatsOnbekend" -> JSONB.fromJson(jsonObject.toString(), VerblijfplaatsOnbekend::class.java)
            "Locatie" -> JSONB.fromJson(jsonObject.toString(), Locatie::class.java)
            else -> throw RuntimeException("Type '%s' wordt niet ondersteund".formatted(type))
        }
    }

    companion object {
        private val JSONB: Jsonb = JsonbBuilder.create(
            JsonbConfig().withPropertyVisibilityStrategy(FieldPropertyVisibilityStrategy())
        )
    }
}
