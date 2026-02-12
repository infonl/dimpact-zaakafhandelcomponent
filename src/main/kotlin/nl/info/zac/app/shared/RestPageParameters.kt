/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.shared

interface RestPageParameters {
    /**
     * The search result page requested, starting at 0.
     */
    val page: Int

    /**
     * The number of search result rows requested.
     */
    val rows: Int
}
