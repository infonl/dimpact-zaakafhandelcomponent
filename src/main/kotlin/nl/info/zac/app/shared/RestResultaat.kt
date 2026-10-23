/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.shared

import jakarta.json.bind.annotation.JsonbProperty

open class RestResultaat<T>(
    @get:JsonbProperty("resultaten")
    open val results: Collection<T> = emptyList(),

    @get:JsonbProperty("totaal")
    open val resultCount: Long = results.size.toLong(),

    @get:JsonbProperty("foutmelding")
    val errorMessage: String = ""
)
