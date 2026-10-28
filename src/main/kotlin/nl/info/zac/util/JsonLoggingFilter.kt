/*
 * SPDX-FileCopyrightText: 2021 Atos, 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util

import jakarta.json.Json
import jakarta.json.bind.JsonbBuilder
import jakarta.json.bind.JsonbConfig
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
            JsonbBuilder.create(JsonbConfig().withFormatting(true)).use { it.toJson(requestContext.entity) }
        } catch (exception: Exception) {
            throw RuntimeException("Failed to create or close JSON builder", exception)
        }

    @Suppress("TooGenericExceptionThrown")
    private fun getPayload(responseContext: ClientResponseContext): String {
        try {
            val payload = IOUtils.toString(responseContext.entityStream, StandardCharsets.UTF_8)
            responseContext.entityStream = IOUtils.toInputStream(payload, StandardCharsets.UTF_8)
            val payloadWriter = StringWriter()
            return try {
                Json.createWriterFactory(mapOf(JsonGenerator.PRETTY_PRINTING to true)).createWriter(payloadWriter).use { jsonWriter ->
                    Json.createReader(StringReader(payload)).use { jsonReader ->
                        jsonWriter.write(jsonReader.read())
                    }
                }
                payloadWriter.toString()
            } catch (_: JsonParsingException) {
                payload
            }
        } catch (ioException: IOException) {
            throw RuntimeException("Error logging REST response", ioException)
        }
    }
}
