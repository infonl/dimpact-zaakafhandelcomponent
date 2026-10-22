/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.util

import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.ext.MessageBodyReader
import jakarta.ws.rs.ext.Provider
import java.io.InputStream
import java.lang.reflect.Type
import java.util.UUID

@Provider
@Produces(MediaType.TEXT_PLAIN)
class UuidReader : MessageBodyReader<UUID> {
    override fun isReadable(
        type: Class<*>,
        genericType: Type,
        annotations: Array<Annotation>,
        mediaType: MediaType
    ) = genericType == UUID::class.java

    override fun readFrom(
        type: Class<UUID>,
        genericType: Type,
        annotations: Array<Annotation>,
        mediaType: MediaType,
        httpHeaders: MultivaluedMap<String, String>,
        entityStream: InputStream
    ): UUID = UUID.fromString(String(entityStream.readAllBytes()))
}
