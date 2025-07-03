/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.shared

import jakarta.validation.constraints.Positive
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
open class RestPageParameters(
    /**
     * The search result page requested, starting at 0.
     */
    open var page: Int,

    /**
     * The number of search result rows requested.
     */
    open var rows: Int
)
