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

import com.fasterxml.jackson.databind.ObjectMapper;

import net.atos.zac.app.configuratie.model.RestTaal;


@Provider
@Produces(MediaType.TEXT_PLAIN)
public class RESTTaalReader implements MessageBodyReader<RestTaal> {

    @Override
    public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return type == RestTaal.class;
    }

    @Override
    public RestTaal readFrom(
            Class<RestTaal> aClass,
            Type type,
            Annotation[] annotations,
            MediaType mediaType,
            MultivaluedMap<String, String> multivaluedMap,
            InputStream inputStream
    ) throws IOException, WebApplicationException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(inputStream, RestTaal.class);
    }
}
