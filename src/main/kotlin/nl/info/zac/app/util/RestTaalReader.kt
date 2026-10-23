/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.util

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.ext.MessageBodyReader
import jakarta.ws.rs.ext.Provider
import nl.info.zac.app.configuration.model.RestTaal
import java.io.InputStream
import java.lang.reflect.Type

@Provider
@Produces(MediaType.TEXT_PLAIN)
class RestTaalReader : MessageBodyReader<RestTaal> {
    override fun isReadable(
        type: Class<*>,
        genericType: Type,
        annotations: Array<Annotation>,
        mediaType: MediaType
    ) = genericType == RestTaal::class.java

    override fun readFrom(
        type: Class<RestTaal>,
        genericType: Type,
        annotations: Array<Annotation>,
        mediaType: MediaType,
        httpHeaders: MultivaluedMap<String, String>,
        entityStream: InputStream
    ): RestTaal? = ObjectMapper().readValue(entityStream, RestTaal::class.java)
}
