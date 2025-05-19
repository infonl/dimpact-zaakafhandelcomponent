/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import nl.info.zac.admin.model.ReferenceTable
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
class RestReferenceTable(
    /**
     * Unique ID of the table. Is null when adding a new table.
     */
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
        // the data model only supports uppercase codes so convert it here to be sure
        code = this@toReferenceTable.code.uppercase()
        name = this@toReferenceTable.naam
        values = this@toReferenceTable.waarden
            .map { referenceTableValue -> referenceTableValue.toReferenceTableValue(this) }
            .toMutableList()
    }
}
