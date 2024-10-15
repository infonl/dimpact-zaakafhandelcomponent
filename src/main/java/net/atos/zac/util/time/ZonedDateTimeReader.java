package net.atos.zac.util.time;

import static java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.ZonedDateTime;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.Provider;

@Provider
@Produces(MediaType.TEXT_PLAIN)
public class ZonedDateTimeReader implements MessageBodyReader<ZonedDateTime> {

    @Override
    public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return type == ZonedDateTime.class;
    }

    @Override
    public ZonedDateTime readFrom(
            Class<ZonedDateTime> aClass,
            Type type,
            Annotation[] annotations,
            MediaType mediaType,
            MultivaluedMap<String, String> multivaluedMap,
            InputStream inputStream
    ) throws IOException, WebApplicationException {
        byte[] zonedDateTimeBytes = inputStream.readAllBytes();
        return ZonedDateTime.parse(new String(zonedDateTimeBytes), ISO_ZONED_DATE_TIME);
    }
}
