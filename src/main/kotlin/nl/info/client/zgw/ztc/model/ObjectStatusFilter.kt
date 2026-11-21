/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.ztc.model

import net.atos.client.zgw.shared.model.AbstractEnum

enum class ObjectStatusFilter(private val value: String) : AbstractEnum<ObjectStatusFilter> {
    /**
     * Toon objecten waarvan het attribuut `concept` true is.
     */
    CONCEPT("concept"),

    /**
     * Toon objecten waarvan het attribuut `concept` false is (standaard).
     */
    DEFINITIEF("definitief"),

    /**
     * Toon objecten waarvan het attribuut `concept` true of false is.
     */
    ALLES("alles");

    override fun toValue(): String {
        return value
    }

    companion object {
        fun fromValue(value: String): ObjectStatusFilter {
            return AbstractEnum.fromValue(entries.toTypedArray(), value)
        }
    }
}
