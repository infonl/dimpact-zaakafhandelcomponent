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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Provider
@Produces(MediaType.TEXT_PLAIN)
class LocalDateReader : MessageBodyReader<LocalDate> {
    private val defaultFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mmXXX")

    override fun isReadable(aClass: Class<*>, type: Type, annotations: Array<Annotation>, mediaType: MediaType): Boolean =
        type == LocalDate::class.java

    @Throws(IOException::class, WebApplicationException::class)
    override fun readFrom(
        aClass: Class<LocalDate>,
        type: Type,
        annotations: Array<Annotation>,
        mediaType: MediaType,
        multivaluedMap: MultivaluedMap<String, String>,
        inputStream: InputStream
    ): LocalDate {
        val localDateBytes = inputStream.readAllBytes()
        return LocalDate.parse(String(localDateBytes), defaultFormatter)
    }
}
