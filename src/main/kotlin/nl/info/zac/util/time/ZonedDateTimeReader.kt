/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util.time

import jakarta.ws.rs.Produces
import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.MultivaluedMap
import jakarta.ws.rs.ext.MessageBodyReader
import jakarta.ws.rs.ext.Provider
import java.io.IOException
import java.io.InputStream
import java.lang.reflect.Type
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME

@Provider
@Produces(MediaType.TEXT_PLAIN)
class ZonedDateTimeReader : MessageBodyReader<ZonedDateTime> {
    override fun isReadable(aClass: Class<*>, type: Type, annotations: Array<Annotation>, mediaType: MediaType): Boolean =
        type == ZonedDateTime::class.java

    @Throws(IOException::class, WebApplicationException::class)
    override fun readFrom(
        aClass: Class<ZonedDateTime>,
        type: Type,
        annotations: Array<Annotation>,
        mediaType: MediaType,
        multivaluedMap: MultivaluedMap<String, String>,
        inputStream: InputStream
    ): ZonedDateTime {
        val zonedDateTimeBytes = inputStream.readAllBytes()
        return ZonedDateTime.parse(String(zonedDateTimeBytes), ISO_ZONED_DATE_TIME)
    }
}
