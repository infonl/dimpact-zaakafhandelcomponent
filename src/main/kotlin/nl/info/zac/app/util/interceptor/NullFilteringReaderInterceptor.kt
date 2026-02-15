/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.util.interceptor

import jakarta.json.Json
import jakarta.json.JsonArray
import jakarta.json.JsonException
import jakarta.json.JsonObject
import jakarta.json.JsonValue
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.ext.Provider
import jakarta.ws.rs.ext.ReaderInterceptor
import jakarta.ws.rs.ext.ReaderInterceptorContext
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.logging.Level
import java.util.logging.Logger

/**
 * A JAX-RS ReaderInterceptor that removes explicit null values from incoming JSON requests.
 *
 * This interceptor solves the issue where JSON-B/Yasson tries to set null values on Kotlin
 * non-nullable properties, which causes a NullPointerException. By removing null values from
 * the JSON before deserialization, these properties are treated as absent and retain their
 * default values (if any) or trigger proper validation errors.
 */
@Provider
class NullFilteringReaderInterceptor : ReaderInterceptor {
    companion object {
        private val LOG = Logger.getLogger(NullFilteringReaderInterceptor::class.java.name)
    }

    override fun aroundReadFrom(context: ReaderInterceptorContext): Any {
        if (context.mediaType?.toString()?.contains(MediaType.APPLICATION_JSON) != true) {
            return context.proceed()
        }
        val jsonString = context.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        if (jsonString.isBlank()) {
            return context.proceed()
        }
        return try {
            val jsonReader = Json.createReader(jsonString.reader())
            val jsonValue = jsonReader.readValue()
            val filteredJsonString = jsonValue.removeNullValues().toString()
            context.inputStream = ByteArrayInputStream(filteredJsonString.toByteArray(StandardCharsets.UTF_8))
            context.proceed()
        } catch (jsonException: JsonException) {
            LOG.log(Level.WARNING, "JSON exception while reading from reader", jsonException)
            // If JSON parsing fails, let the original stream through for proper error handling
            context.inputStream = ByteArrayInputStream(jsonString.toByteArray(StandardCharsets.UTF_8))
            context.proceed()
        }
    }

    private fun JsonValue.removeNullValues(): JsonValue = when (this.valueType) {
        JsonValue.ValueType.OBJECT -> this.asJsonObject().removeNullValuesFromObject()
        JsonValue.ValueType.ARRAY -> this.asJsonArray().removeNullValuesFromArray()
        else -> this
    }

    private fun JsonObject.removeNullValuesFromObject(): JsonObject {
        val builder = Json.createObjectBuilder()
        this.forEach { (key, value) ->
            if (value.valueType != JsonValue.ValueType.NULL) {
                builder.add(key, value.removeNullValues())
            }
        }
        return builder.build()
    }

    private fun JsonArray.removeNullValuesFromArray(): JsonArray {
        val builder = Json.createArrayBuilder()
        this.forEach { value ->
            if (value.valueType != JsonValue.ValueType.NULL) {
                builder.add(value.removeNullValues())
            }
        }
        return builder.build()
    }
}
