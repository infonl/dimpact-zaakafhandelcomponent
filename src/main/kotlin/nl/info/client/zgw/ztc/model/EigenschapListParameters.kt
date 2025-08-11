/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.ztc.model

import jakarta.json.bind.annotation.JsonbTypeDeserializer
import jakarta.json.bind.annotation.JsonbTypeSerializer
import jakarta.json.bind.serializer.DeserializationContext
import jakarta.json.bind.serializer.JsonbDeserializer
import jakarta.json.bind.serializer.JsonbSerializer
import jakarta.json.bind.serializer.SerializationContext
import jakarta.json.stream.JsonGenerator
import jakarta.json.stream.JsonParser
import jakarta.ws.rs.QueryParam
import java.lang.reflect.Type
import java.net.URI

@JsonbTypeSerializer(EigenschapListParametersStatus.Serializer::class)
@JsonbTypeDeserializer(EigenschapListParametersStatus.Deserializer::class)
enum class EigenschapListParametersStatus(val value: String) {
    CONCEPT("concept"),
    DEFINITIEF("definitief"),
    ALLES("alles");

    override fun toString(): String = value

    class Deserializer : JsonbDeserializer<EigenschapListParametersStatus> {
        override fun deserialize(parser: JsonParser, ctx: DeserializationContext, rtType: Type): EigenschapListParametersStatus {
            return fromValue(parser.string)
        }
    }

    class Serializer : JsonbSerializer<EigenschapListParametersStatus> {
        override fun serialize(obj: EigenschapListParametersStatus, generator: JsonGenerator, ctx: SerializationContext) {
            generator.write(obj.value)
        }
    }

    companion object {
        fun fromValue(text: String): EigenschapListParametersStatus {
            for (b in values()) {
                if (b.value == text) {
                    return b
                }
            }
            throw IllegalArgumentException("Unexpected value '$text'")
        }
    }
}

class EigenschapListParameters {
    @field:QueryParam("zaaktype")
    var zaaktype: URI? = null

    @field:QueryParam("zaaktypeIdentificatie")
    var zaaktypeIdentificatie: String? = null

    @field:QueryParam("status")
    var status: EigenschapListParametersStatus? = null
}
