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
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.drc.model.generated.StatusEnum
import java.io.InputStream
import java.lang.reflect.Type

@Provider
@Produces(MediaType.TEXT_PLAIN)
class EnkelvoudigInformatieObjectStatusEnumReader : MessageBodyReader<StatusEnum> {
    companion object {
        private val CLASS_PREFIX = EnkelvoudigInformatieObject::class.java.toString().replace("class ", "") + ".StatusEnum."
    }

    override fun isReadable(
        type: Class<*>,
        genericType: Type,
        annotations: Array<Annotation>,
        mediaType: MediaType
    ) = genericType == StatusEnum::class.java

    override fun readFrom(
        type: Class<StatusEnum>,
        genericType: Type,
        annotations: Array<Annotation>,
        mediaType: MediaType,
        httpHeaders: MultivaluedMap<String, String>,
        entityStream: InputStream
    ): StatusEnum = StatusEnum.valueOf(String(entityStream.readAllBytes()).removePrefix(CLASS_PREFIX).uppercase())
}
