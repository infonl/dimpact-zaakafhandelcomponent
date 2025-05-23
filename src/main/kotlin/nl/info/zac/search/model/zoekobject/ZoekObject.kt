/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.model.zoekobject

interface ZoekObject {
    companion object {
        const val IS_TOEGEKEND_FIELD: String = "isToegekend"
    }

    fun getObjectId(): String

    fun getType(): ZoekObjectType
}
