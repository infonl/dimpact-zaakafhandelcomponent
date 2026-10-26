/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util

import jakarta.json.bind.Jsonb
import jakarta.json.bind.JsonbBuilder
import jakarta.json.bind.JsonbConfig
import jakarta.json.bind.config.PropertyVisibilityStrategy
import java.lang.reflect.Field
import java.lang.reflect.Method

val JSONB: Jsonb = JsonbBuilder.create()

val FIELD_VISIBILITY_STRATEGY: Jsonb = JsonbBuilder.create(
    JsonbConfig().withPropertyVisibilityStrategy(object : PropertyVisibilityStrategy {
        override fun isVisible(field: Field) = true

        override fun isVisible(method: Method) = false
    })
)
