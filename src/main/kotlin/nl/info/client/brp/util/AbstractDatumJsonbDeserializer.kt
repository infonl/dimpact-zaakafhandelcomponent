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
import nl.info.client.brp.model.generated.AbstractDatum
import nl.info.client.brp.model.generated.DatumOnbekend
import nl.info.client.brp.model.generated.JaarDatum
import nl.info.client.brp.model.generated.JaarMaandDatum
import nl.info.client.brp.model.generated.VolledigeDatum
import nl.info.zac.exception.InputValidationFailedException
import java.lang.reflect.Type

class AbstractDatumJsonbDeserializer : JsonbDeserializer<AbstractDatum> {
    companion object {
        private val JSONB: Jsonb = JsonbBuilder.create(
            JsonbConfig().withPropertyVisibilityStrategy(FieldPropertyVisibilityStrategy())
        )
    }

    override fun deserialize(parser: JsonParser, ctx: DeserializationContext, rtType: Type): AbstractDatum =
        parser.getObject().let {
            when (val type = it.getString("type")) {
                "Datum" -> JSONB.fromJson(it.toString(), VolledigeDatum::class.java)
                "DatumOnbekend" -> JSONB.fromJson(it.toString(), DatumOnbekend::class.java)
                "JaarDatum" -> JSONB.fromJson(it.toString(), JaarDatum::class.java)
                "JaarMaandDatum" -> JSONB.fromJson(it.toString(), JaarMaandDatum::class.java)
                else -> throw InputValidationFailedException(message = "Type '$type' is not supported")
            }
        }
}
