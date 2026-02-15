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
import jakarta.ws.rs.WebApplicationException
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

    @Throws(WebApplicationException::class)
    override fun aroundReadFrom(context: ReaderInterceptorContext): Any? {
        val mediaType = context.mediaType
        if (mediaType == null || !mediaType.toString().contains(MediaType.APPLICATION_JSON)) {
            return context.proceed()
        }
        val inputStream = context.inputStream
        val jsonString = inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        if (jsonString.isBlank()) {
            return context.proceed()
        }
        return try {
            val jsonReader = Json.createReader(jsonString.reader())
            val jsonValue = jsonReader.readValue()
            val filteredJson = removeNulls(jsonValue)
            val filteredJsonString = filteredJson.toString()
            context.inputStream = ByteArrayInputStream(filteredJsonString.toByteArray(StandardCharsets.UTF_8))
        } catch (jsonException: JsonException) {
            LOG.log(Level.WARNING, "JSON exception while reading from reader", jsonException)
            // If JSON parsing fails, let the original stream through for proper error handling
            context.inputStream = ByteArrayInputStream(jsonString.toByteArray(StandardCharsets.UTF_8))
        } finally {
            context.proceed()
        }
    }

    private fun removeNulls(jsonValue: JsonValue): JsonValue = when (jsonValue.valueType) {
        JsonValue.ValueType.OBJECT -> removeNullsFromObject(jsonValue.asJsonObject())
        JsonValue.ValueType.ARRAY -> removeNullsFromArray(jsonValue.asJsonArray())
        else -> jsonValue
    }

    private fun removeNullsFromObject(jsonObject: JsonObject): JsonObject {
        val builder = Json.createObjectBuilder()
        jsonObject.forEach { (key, value) ->
            if (value.valueType != JsonValue.ValueType.NULL) {
                builder.add(key, removeNulls(value))
            }
        }
        return builder.build()
    }

    private fun removeNullsFromArray(jsonArray: JsonArray): JsonArray {
        val builder = Json.createArrayBuilder()
        jsonArray.forEach { value ->
            if (value.valueType != JsonValue.ValueType.NULL) {
                builder.add(removeNulls(value))
            }
        }
        return builder.build()
    }
}
