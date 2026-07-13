/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import nl.info.zac.admin.model.ReferenceTable
import nl.info.zac.admin.model.ReferenceTableValue
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
class RestReferenceTableValue(
    /**
     * Unique ID of the table value. Is null when adding a new table value.
     */
    var id: Long? = null,

    @field:NotBlank
    @field:Size(max = REFERENCE_TABLE_VALUE_MAX_LENGTH)
    var name: String,

    var isSystemValue: Boolean = false
) {
    companion object {
        const val REFERENCE_TABLE_VALUE_MAX_LENGTH = 1000
    }
}

fun RestReferenceTableValue.toReferenceTableValue(
    referenceTable: ReferenceTable
): ReferenceTableValue = ReferenceTableValue().apply {
    id = this@toReferenceTableValue.id
    name = this@toReferenceTableValue.name
    isSystemValue = this@toReferenceTableValue.isSystemValue
    this.referenceTable = referenceTable
}
