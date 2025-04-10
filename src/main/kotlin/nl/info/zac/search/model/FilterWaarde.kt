/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.model

enum class FilterWaarde(private val magicValue: String) {
    LEEG("-NULL-"),
    NIET_LEEG("-NOT-NULL-");

    override fun toString() = magicValue

    fun <TYPE> isEqualTo(value: TYPE?): Boolean = value != null && value.toString() == magicValue
}
