/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.util

import jakarta.json.bind.serializer.DeserializationContext
import jakarta.json.bind.serializer.JsonbDeserializer
import jakarta.json.stream.JsonParser
import org.apache.commons.lang3.StringUtils
import java.lang.reflect.Type
import java.net.URI
import java.net.URISyntaxException

class URIJsonbDeserializer : JsonbDeserializer<URI> {
    @Suppress("SwallowedException", "TooGenericExceptionThrown")
    override fun deserialize(parser: JsonParser, ctx: DeserializationContext, rtType: Type): URI? =
        try {
            val uri = parser.string
            if (StringUtils.isNotEmpty(uri)) URI(uri) else null
        } catch (illegalStateException: IllegalStateException) {
            // ignore this exception for now. workaround for the following error:
            // "RESTEASY008200: JSON Binding deserialization error: jakarta.json.bind.JsonbException:
            // Internal error: JsonParser#getString() is valid only for KEY_NAME, VALUE_STRING,
            // VALUE_NUMBER parser states. But the current parser state is VALUE_NULL"
            null
        } catch (uriSyntaxException: URISyntaxException) {
            throw RuntimeException(uriSyntaxException)
        }
}
