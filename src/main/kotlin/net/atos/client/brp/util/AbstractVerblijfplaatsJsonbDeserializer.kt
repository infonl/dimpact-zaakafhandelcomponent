/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 Lifely
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
    companion object {
        private val JSONB: Jsonb = JsonbBuilder.create(
            JsonbConfig().withPropertyVisibilityStrategy(FieldPropertyVisibilityStrategy())
        )
    }

    override fun deserialize(
        parser: JsonParser,
        ctx: DeserializationContext,
        rtType: Type
    ): AbstractVerblijfplaats =
        parser.getObject().let {
            when (val type = it.getString("type")) {
                "VerblijfplaatsBuitenland" -> JSONB.fromJson(
                    it.toString(),
                    VerblijfplaatsBuitenland::class.java
                )
                "Adres" -> JSONB.fromJson(it.toString(), Adres::class.java)
                "VerblijfplaatsOnbekend" -> JSONB.fromJson(it.toString(), VerblijfplaatsOnbekend::class.java)
                "Locatie" -> JSONB.fromJson(it.toString(), Locatie::class.java)
                else -> throw RuntimeException(
                    "Unsupported ${AbstractVerblijfplaats::class.java.simpleName} type: '$type'"
                )
            }
        }
}
