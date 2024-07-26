/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.model

import jakarta.validation.constraints.NotBlank
import nl.lifely.zac.util.AllOpen

@AllOpen
class RestReferenceTable {
    var id: Long? = null

    @NotBlank
    lateinit var code: String

    @NotBlank
    lateinit var name: String

    val isSystemReferenceTable: Boolean = false

    var valuesCount: Int = 0

    var values: List<RestReferenceTableValue>? = null
}
