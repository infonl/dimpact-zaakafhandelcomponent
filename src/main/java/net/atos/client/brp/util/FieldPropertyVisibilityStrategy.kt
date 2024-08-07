/*
 * SPDX-FileCopyrightText: 2023 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.brp.util

import jakarta.json.bind.config.PropertyVisibilityStrategy
import java.lang.reflect.Field
import java.lang.reflect.Method

class FieldPropertyVisibilityStrategy : PropertyVisibilityStrategy {
    override fun isVisible(field: Field): Boolean {
        return true
    }

    override fun isVisible(method: Method): Boolean {
        return false
    }
}
