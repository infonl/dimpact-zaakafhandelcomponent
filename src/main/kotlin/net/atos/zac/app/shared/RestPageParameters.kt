/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.shared

import jakarta.validation.constraints.Positive
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
open class RestPageParameters(
    @JvmField @Positive var page: Int,
    @JvmField @Positive var rows: Int
)
