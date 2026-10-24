/*
 * SPDX-FileCopyrightText: 2023 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.brp.util

import jakarta.json.bind.Jsonb
import jakarta.json.bind.JsonbBuilder
import jakarta.json.bind.JsonbConfig
import jakarta.json.bind.serializer.DeserializationContext
import jakarta.json.bind.serializer.JsonbDeserializer
import jakarta.json.stream.JsonParser
import nl.info.client.brp.model.generated.AbstractVerblijfplaats
import nl.info.client.brp.model.generated.Adres
import nl.info.client.brp.model.generated.Locatie
import nl.info.client.brp.model.generated.VerblijfplaatsBuitenland
import nl.info.client.brp.model.generated.VerblijfplaatsOnbekend
import nl.info.zac.exception.InputValidationFailedException
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
                else -> throw InputValidationFailedException(
                    message = "Unsupported ${AbstractVerblijfplaats::class.java.simpleName} type: '$type'"
                )
            }
        }
}
