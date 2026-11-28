/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;

import org.apache.commons.lang3.StringUtils;

import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject;
import nl.info.client.zgw.drc.model.generated.StatusEnum;

@Provider
@Produces(MediaType.TEXT_PLAIN)
public class EnkelvoudigInformatieObjectStatusEnumReader implements MessageBodyReader<StatusEnum> {

    private static final String CLASS_PREFIX = EnkelvoudigInformatieObject.class.toString().replace("class ", "") + ".StatusEnum.";

    @Override
    public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return type == StatusEnum.class;
    }

    @Override
    public StatusEnum readFrom(
            Class<StatusEnum> aClass,
            Type type,
            Annotation[] annotations,
            MediaType mediaType,
            MultivaluedMap<String, String> multivaluedMap,
            InputStream inputStream
    ) throws IOException, WebApplicationException {
        String enumValueAsString = new String(inputStream.readAllBytes());
        enumValueAsString = StringUtils.removeStart(enumValueAsString, CLASS_PREFIX);

        return StatusEnum.valueOf(enumValueAsString.toUpperCase());
    }
}
