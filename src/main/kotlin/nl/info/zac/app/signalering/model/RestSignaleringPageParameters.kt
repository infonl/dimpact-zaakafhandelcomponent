/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.signalering.model

import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import nl.info.zac.app.shared.RestPageParameters
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
data class RestSignaleringPageParameters(
    @field:PositiveOrZero
    override var page: Int,

    @field: Positive
    override var rows: Int
) : RestPageParameters