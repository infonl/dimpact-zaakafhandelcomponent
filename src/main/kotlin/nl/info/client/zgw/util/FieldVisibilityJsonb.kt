/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.util

import jakarta.json.bind.Jsonb
import jakarta.json.bind.JsonbBuilder
import jakarta.json.bind.JsonbConfig
import jakarta.json.bind.config.PropertyVisibilityStrategy
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Field-visible Jsonb instance used internally by custom JSON-B deserializers to parse nested subtype JSON.
 *
 * Must stay separate from the Jsonb instance configured in [JsonbConfiguration]: reusing that instance would cause infinite recursion,
 * since a subtype deserializer would re-invoke itself on its own base type. Also required because the model classes it deserializes
 * expose no getters, only fields.
 */
val JSONB: Jsonb = JsonbBuilder.create(
    JsonbConfig()
        .withPropertyVisibilityStrategy(object : PropertyVisibilityStrategy {
            override fun isVisible(field: Field): Boolean = true

            override fun isVisible(method: Method): Boolean = false
        })
)
