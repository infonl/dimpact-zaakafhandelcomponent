/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.zoeken.model.zoekobject

interface ZoekObject {
    companion object {
        const val IS_TOEGEKEND_FIELD: String = "isToegekend"
    }

    fun getId(): String

    fun getType(): ZoekObjectType
}
