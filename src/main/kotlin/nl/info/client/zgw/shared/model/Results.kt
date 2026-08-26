/*
 * SPDX-FileCopyrightText: 2021 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.shared.model

import jakarta.json.bind.annotation.JsonbTypeDeserializer
import nl.info.client.zgw.util.ResultsJsonbDeserializer
import java.net.URI

/**
 * Deserialized via [ResultsJsonbDeserializer] rather than an automatically-bound `@JsonbCreator` constructor:
 * all ZGW REST clients share a single `Jsonb` instance (see `JsonbConfiguration`), and Yasson caches a generic
 * class's resolved creator/item type per raw class rather than per concrete instantiation. With an
 * automatically-bound creator, deserializing `Results<Catalogus>` followed by `Results<Eigenschap>` through that
 * shared instance silently returns `Eigenschap`-shaped data still typed (and cast) as `Catalogus`, or vice versa.
 * A real Java record does not have this problem — Yasson resolves records per call site — but Kotlin has no
 * record equivalent, so the item type is resolved manually instead, the same way `AuditWijziging` resolves its
 * own type parameter via `AuditWijzigingJsonbDeserializer`.
 */
@JsonbTypeDeserializer(ResultsJsonbDeserializer::class)
data class Results<T>(
    private val countValue: Int,
    private val resultsValue: List<T>?,
    private val nextValue: URI? = null,
    private val previousValue: URI? = null
) {
    companion object {
        // Default value for the `pageSize` in both the ZGW ZRC and ZGW ZTC APIs
        const val DEFAULT_ZGW_PAGE_SIZE: Long = 100
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
