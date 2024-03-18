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

import net.atos.zac.app.configuratie.model.RESTTaal;


@Provider
@Produces(MediaType.TEXT_PLAIN)
public class RESTTaalReader implements MessageBodyReader<RESTTaal> {

    @Override
    public boolean isReadable(Class<?> aClass, Type type, Annotation[] annotations, MediaType mediaType) {
        return type == RESTTaal.class;
    }

    @Override
    public RESTTaal readFrom(
            Class<RESTTaal> aClass,
            Type type,
            Annotation[] annotations,
            MediaType mediaType,
            MultivaluedMap<String, String> multivaluedMap,
            InputStream inputStream
    ) throws IOException, WebApplicationException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(inputStream, RESTTaal.class);
    }
}
