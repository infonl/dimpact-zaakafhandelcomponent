/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.model

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import net.atos.zac.admin.model.ReferenceTable
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
class RestReferenceTable(
    var id: Long? = null,

    @field:NotBlank
    var code: String,

    @field:NotBlank
    var naam: String,

    var systeem: Boolean = false,

    var aantalWaarden: Int = 0,

    @field:Valid
    var waarden: List<RestReferenceTableValue> = emptyList()
)

fun RestReferenceTable.toReferenceTable(): ReferenceTable {
    return ReferenceTable().apply {
        code = this@toReferenceTable.code
        name = this@toReferenceTable.naam
        values = this@toReferenceTable.waarden
            .map { referenceTableValue -> referenceTableValue.toReferenceTableValue(this) }
            .toMutableList()
    }
}

