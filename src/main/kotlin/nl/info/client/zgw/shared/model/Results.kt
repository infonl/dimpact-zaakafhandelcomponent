/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared.model

import jakarta.json.bind.annotation.JsonbCreator
import jakarta.json.bind.annotation.JsonbProperty
import java.net.URI

data class Results<T> @JsonbCreator constructor(
    @param:JsonbProperty("count") private val countValue: Int,
    @param:JsonbProperty("results") private val resultsValue: List<T>?,
    @param:JsonbProperty("next") private val nextValue: URI? = null,
    @param:JsonbProperty("previous") private val previousValue: URI? = null
) {
    companion object {
        // Number of items Open Zaak returns per page
        const val NUM_ITEMS_PER_PAGE: Long = 100
    }

    constructor(results: List<T>, count: Int) : this(count, results, null, null)

    fun count(): Int = countValue

    fun results(): List<T> = resultsValue ?: emptyList()

    fun next(): URI? = nextValue

    fun previous(): URI? = previousValue

    val singleResult: T?
        get() {
            val results = results()
            return when {
                results.isEmpty() -> null
                results.size == 1 -> results.first()
                else -> throw IllegalStateException("More than one result found (count: $countValue)")
            }
        }

    val singlePageResults: List<T>
        get() = if (nextValue == null) {
            results()
        } else {
            throw IllegalStateException("More than one page found (count: $countValue, results: ${results().size})")
        }
}
