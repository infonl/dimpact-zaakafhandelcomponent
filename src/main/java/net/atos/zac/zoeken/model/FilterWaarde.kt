/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.zoeken.model

enum class FilterWaarde(private val magicValue: String) {
    LEEG("-NULL-"),
    NIET_LEEG("-NOT-NULL-");

    override fun toString() = magicValue

    fun <TYPE> `is`(value: TYPE?): Boolean = value != null && value.toString() == magicValue
}
