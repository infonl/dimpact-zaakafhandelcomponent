/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.model

import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
data class FilterParameters(
    var values: List<String>,

    var inverse: Boolean
)
