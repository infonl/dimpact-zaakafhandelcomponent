/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.util

import jakarta.json.JsonObject
import jakarta.json.bind.serializer.DeserializationContext
import jakarta.json.bind.serializer.JsonbDeserializer
import jakarta.json.stream.JsonParser
import nl.info.client.zgw.shared.model.Results
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.net.URI

/**
 * Resolves the `T` in `Results<T>` from the call site's parameterized type (`rtType`) instead of relying on
 * Yasson's automatic `@JsonbCreator` binding. See the KDoc on [Results] for why that matters.
 */
class ResultsJsonbDeserializer : JsonbDeserializer<Results<*>> {
    override fun deserialize(parser: JsonParser, ctx: DeserializationContext, rtType: Type): Results<*>? {
        val jsonObject = parser.value as? JsonObject ?: return null
        val itemType = (rtType as ParameterizedType).actualTypeArguments[0]

        val results = if (jsonObject.containsKey("results") && jsonObject.isNull("results")) {
            null
        } else {
            jsonObject.getJsonArray("results")?.map { JSONB.fromJson<Any>(it.toString(), itemType) }
        }
        return Results(
            jsonObject.getInt("count"),
            results,
            jsonObject.uriOrNull("next"),
            jsonObject.uriOrNull("previous")
        )
    }

    private fun JsonObject.uriOrNull(name: String): URI? =
if (!containsKey(name) || isNull(name)) null else getString(name).takeIf { it.isNotEmpty() }?.let(::URI)
}
