/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util.time

import jakarta.ws.rs.ext.ParamConverter
import jakarta.ws.rs.ext.ParamConverterProvider
import jakarta.ws.rs.ext.Provider
import java.lang.reflect.Type
import java.time.ZonedDateTime

/**
 * Provider for ZonedDateTime jakarta web-service parameter converter.
 *
 * @see ZonedDateTimeParamConverter
 */
@Provider
class ZonedDateTimeParamConverterProvider : ParamConverterProvider {
    @Suppress("UNCHECKED_CAST")
    override fun <T> getConverter(rawType: Class<T>, genericType: Type, annotations: Array<Annotation>): ParamConverter<T>? =
        if (rawType.isAssignableFrom(ZonedDateTime::class.java)) {
            ZonedDateTimeParamConverter() as ParamConverter<T>
        } else {
            null
        }
}
