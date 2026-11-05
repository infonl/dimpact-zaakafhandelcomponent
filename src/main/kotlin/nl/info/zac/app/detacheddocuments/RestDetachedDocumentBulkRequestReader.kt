/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.detacheddocuments

import jakarta.json.Json
import jakarta.json.JsonArray
import jakarta.json.JsonException
import jakarta.json.JsonNumber
import jakarta.json.JsonObject
import jakarta.json.JsonValue
import jakarta.json.stream.JsonParsingException
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.ext.MessageBodyReader
import jakarta.ws.rs.ext.Provider
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentBulkDeleteRequest
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentBulkLinkRequest
import java.io.InputStream
import java.lang.reflect.Type

/**
 * JSON-B truncates `1.5` to 1 and wraps a number beyond the range of a Long, so a bad id would delete an unrelated
 * document that cannot be restored. Reads the bulk requests itself, and refuses such ids with a 400.
 */
@Provider
@Consumes(MediaType.APPLICATION_JSON)
class RestDetachedDocumentBulkRequestReader : MessageBodyReader<Any> {
    override fun isReadable(
        type: Class<*>,
        genericType: Type,
        annotations: Array<Annotation>,
        mediaType: MediaType
    ) = type == RestDetachedDocumentBulkDeleteRequest::class.java ||
        type == RestDetachedDocumentBulkLinkRequest::class.java

    override fun readFrom(
        type: Class<Any>,
        genericType: Type,
        annotations: Array<Annotation>,
        mediaType: MediaType,
        httpHeaders: MultivaluedMap<String, String>,
        entityStream: InputStream
    ): Any {
        val body = entityStream.readJsonObject()
        val ids = body.readIds()
        return if (type == RestDetachedDocumentBulkDeleteRequest::class.java) {
            RestDetachedDocumentBulkDeleteRequest(ids = ids)
        } else {
            RestDetachedDocumentBulkLinkRequest(ids = ids, nieuweZaakID = body.getString("nieuweZaakID", ""))
        }
    }

    private fun InputStream.readJsonObject(): JsonObject =
        try {
            Json.createReader(this).use { it.readObject() }
        } catch (jsonParsingException: JsonParsingException) {
            throw BadRequestException("The request body is not a valid JSON object", jsonParsingException)
        } catch (jsonException: JsonException) {
            throw BadRequestException("The request body is not a valid JSON object", jsonException)
        }

    // Missing ids are read as an empty list, which the validation of the request refuses.
    private fun JsonObject.readIds(): List<Long> =
        when (val ids = this["ids"]) {
            null, JsonValue.NULL -> emptyList()
            is JsonArray -> ids.map { it.toId() }
            else -> throw BadRequestException("The ids must be a list of whole numbers")
        }

    private fun JsonValue.toId(): Long =
        try {
            (this as? JsonNumber)?.takeIf { it.isIntegral }?.longValueExact()
        } catch (arithmeticException: ArithmeticException) {
            throw BadRequestException("The id '$this' is too large", arithmeticException)
        } ?: throw BadRequestException("The id '$this' is not a whole number")
}
