/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared.model

import jakarta.json.bind.adapter.JsonbAdapter

interface AbstractEnum {

    fun toValue(): String

    companion object {
        fun <T : AbstractEnum> fromValue(enums: Array<T>, value: String): T =
            enums.first { it.toValue() == value }
    }

    abstract class Adapter<T : AbstractEnum> : JsonbAdapter<T, String> {

        override fun adaptToJson(anEnum: T): String = anEnum.toValue()

        override fun adaptFromJson(json: String): T? =
            getEnums().firstOrNull { it.toValue() == json }

        protected abstract fun getEnums(): Array<T>
    }
}
