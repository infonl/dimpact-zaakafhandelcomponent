package net.atos.zac.app.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.UUID;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;


@Provider
@Produces(MediaType.TEXT_PLAIN)
public class UUIDReader implements MessageBodyReader<UUID> {

    @Override
    public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return type == UUID.class;
    }

    @Override
    public UUID readFrom(
            Class<UUID> aClass,
            Type type,
            Annotation[] annotations,
            MediaType mediaType,
            MultivaluedMap<String, String> multivaluedMap,
            InputStream inputStream
    ) throws IOException, WebApplicationException {
        if (inputStream == null) {
            return null;
        }

        byte[] uuidBytes = inputStream.readAllBytes();
        if (uuidBytes.length == 0) {
            return null;
        }

        return UUID.fromString(new String(uuidBytes));
    }
}
