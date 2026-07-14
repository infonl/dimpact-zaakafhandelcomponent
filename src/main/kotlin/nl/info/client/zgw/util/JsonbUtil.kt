/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.util

import jakarta.json.bind.Jsonb
import jakarta.json.bind.JsonbBuilder
import jakarta.json.bind.JsonbConfig
import jakarta.json.bind.config.PropertyVisibilityStrategy
import java.lang.reflect.Field
import java.lang.reflect.Method

val JSONB: Jsonb = JsonbBuilder.create(
    JsonbConfig()
        .withPropertyVisibilityStrategy(object : PropertyVisibilityStrategy {
            override fun isVisible(field: Field): Boolean = true

            override fun isVisible(method: Method): Boolean = false
        })
)
