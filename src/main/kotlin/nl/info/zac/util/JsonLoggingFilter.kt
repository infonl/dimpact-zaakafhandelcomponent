/*
 * SPDX-FileCopyrightText: 2021 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util

import jakarta.json.Json
import jakarta.json.JsonArray
import jakarta.json.JsonObject
import jakarta.json.JsonValue
import jakarta.json.bind.JsonbBuilder
import jakarta.json.stream.JsonGenerator
import jakarta.json.stream.JsonParsingException
import jakarta.ws.rs.client.ClientRequestContext
import jakarta.ws.rs.client.ClientRequestFilter
import jakarta.ws.rs.client.ClientResponseContext
import jakarta.ws.rs.client.ClientResponseFilter
import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.ext.Provider
import org.apache.commons.io.IOUtils
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.util.logging.Level
import java.util.logging.Logger

@Provider
class JsonLoggingFilter : ClientRequestFilter, ClientResponseFilter {
    companion object {
        private val LOG_LEVEL = Level.FINE
        private val LOG = Logger.getLogger(JsonLoggingFilter::class.java.name)

        // BSNs and IBANs are masked by the WildFly logging filter, but a date of birth cannot be recognised in
        // plain text, so it is masked here by the name of its JSON field; see ADR-0010
        private const val DATE_OF_BIRTH_FIELD_NAME_PART = "geboorte"
        const val MASKED_VALUE = "*****"
        const val WITHHELD_PAYLOAD = "[not logged, because the payload is not valid JSON and may contain a date of birth]"
    }

    override fun filter(requestContext: ClientRequestContext) {
        if (LOG.isLoggable(LOG_LEVEL)) {
            val message = StringBuilder("REST Request\n")
            message.append("URL: ${requestContext.uri}\n")
            message.append("Method: ${requestContext.method}\n")
            message.append("Media type: ${requestContext.mediaType}\n")
            message.appendHeaders(requestContext.headers)
            if (requestContext.hasEntity()) {
                message.append("Payload:")
                message.append(getPayload(requestContext))
            }
            LOG.log(LOG_LEVEL, message.toString())
        }
    }

    override fun filter(requestContext: ClientRequestContext, responseContext: ClientResponseContext) {
        if (LOG.isLoggable(LOG_LEVEL)) {
            val message = StringBuilder("REST Response\n")
            message.append(
                "Status: ${responseContext.statusInfo.statusCode} (${responseContext.statusInfo.reasonPhrase})\n"
            )
            message.append("Media type: ${responseContext.mediaType}\n")
            message.appendHeaders(responseContext.headers)
            if (responseContext.hasEntity()) {
                message.append("Payload:")
                message.append(getPayload(responseContext))
            }
            LOG.log(LOG_LEVEL, message.toString())
        }
    }

    private fun StringBuilder.appendHeaders(headers: MultivaluedMap<String, *>) {
        append("Headers:\n")
        headers.forEach { (header, value) -> append("   $header : $value\n") }
    }

    @Suppress("TooGenericExceptionCaught", "TooGenericExceptionThrown")
    private fun getPayload(requestContext: ClientRequestContext): String =
        try {
            JsonbBuilder.create().use { it.toJson(requestContext.entity) }.toPrettyJsonWithMaskedDatesOfBirth()
        } catch (exception: Exception) {
            throw RuntimeException("Failed to create or close JSON builder", exception)
        }

    @Suppress("TooGenericExceptionThrown")
    private fun getPayload(responseContext: ClientResponseContext): String {
        try {
            val payload = IOUtils.toString(responseContext.entityStream, StandardCharsets.UTF_8)
            responseContext.entityStream = IOUtils.toInputStream(payload, StandardCharsets.UTF_8)
            return payload.toPrettyJsonWithMaskedDatesOfBirth()
        } catch (ioException: IOException) {
            throw RuntimeException("Error logging REST response", ioException)
        }
    }

    private fun String.toPrettyJsonWithMaskedDatesOfBirth(): String =
        try {
            StringWriter().also { payloadWriter ->
                Json.createWriterFactory(mapOf(JsonGenerator.PRETTY_PRINTING to true)).createWriter(payloadWriter).use { jsonWriter ->
                    Json.createReader(StringReader(this)).use { jsonReader ->
                        jsonWriter.write(jsonReader.readValue().maskDatesOfBirth())
                    }
                }
            }.toString()
        } catch (_: JsonParsingException) {
            if (contains(DATE_OF_BIRTH_FIELD_NAME_PART, ignoreCase = true)) WITHHELD_PAYLOAD else this
        }

    private fun JsonValue.maskDatesOfBirth(): JsonValue =
        when (this) {
            is JsonObject -> Json.createObjectBuilder().apply {
                this@maskDatesOfBirth.forEach { (name, value) ->
                    add(
                        name,
                        if (name.contains(DATE_OF_BIRTH_FIELD_NAME_PART, ignoreCase = true)) {
                            Json.createValue(MASKED_VALUE)
                        } else {
                            value.maskDatesOfBirth()
                        }
                    )
                }
            }.build()
            is JsonArray -> Json.createArrayBuilder().apply {
                this@maskDatesOfBirth.forEach { add(it.maskDatesOfBirth()) }
            }.build()
            else -> this
        }
}
