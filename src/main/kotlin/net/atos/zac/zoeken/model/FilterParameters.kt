/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.zoeken.model

import jakarta.json.bind.annotation.JsonbProperty
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
data class FilterParameters(
    @field:JsonbProperty("waarden")
    var values: List<String>,

    var inverse: Boolean
)
