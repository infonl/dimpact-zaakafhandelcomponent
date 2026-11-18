/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util.time

import jakarta.ws.rs.ext.ParamConverter
import jakarta.ws.rs.ext.ParamConverterProvider
import jakarta.ws.rs.ext.Provider
import java.lang.reflect.Type
import java.time.LocalDate

@Provider
class LocalDateParamConverterProvider : ParamConverterProvider {
    @Suppress("UNCHECKED_CAST")
    override fun <T> getConverter(rawType: Class<T>, genericType: Type, annotations: Array<Annotation>): ParamConverter<T>? =
        if (rawType.isAssignableFrom(LocalDate::class.java)) {
            LocalDateParamConverter() as ParamConverter<T>
        } else {
            null
        }
}
