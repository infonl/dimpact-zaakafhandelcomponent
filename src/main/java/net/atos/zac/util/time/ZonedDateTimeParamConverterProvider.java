package net.atos.zac.util.time;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.ZonedDateTime;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

/**
 * Solves missing valueOf() and string constructor for ZonedDateTime class
 */
@Provider
public class ZonedDateTimeParamConverterProvider implements ParamConverterProvider {

    @SuppressWarnings("unchecked")
    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (rawType.isAssignableFrom(ZonedDateTime.class)) {
            return (ParamConverter<T>) new ZonedDateTimeParamConverter();
        }
        return null;
    }
}
