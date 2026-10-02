/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.model

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
data class FilterParameters(
    var values: List<String>,

    @get:JsonbProperty("inverse")
    @set:JsonbProperty("inverse")
    var isInverse: Boolean = false
)
