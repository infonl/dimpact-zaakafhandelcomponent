/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.shared

import jakarta.validation.constraints.Positive
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
open class RestPageParameters(
    @Positive open var page: Int,
    @Positive open var rows: Int
)
