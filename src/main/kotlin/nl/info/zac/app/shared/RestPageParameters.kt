/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.shared

interface RestPageParameters {
    /**
     * The search result page requested, starting at 0.
     */
    var page: Int

    /**
     * The number of search result rows requested.
     */
    var rows: Int
}

fun <T> List<T>.takePage(restPageParameters: RestPageParameters): List<T> =
    drop((restPageParameters.page.toLong() * restPageParameters.rows).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
        .take(restPageParameters.rows)
