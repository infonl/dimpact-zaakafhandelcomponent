/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.model

import jakarta.validation.constraints.NotBlank
import net.atos.zac.admin.model.ReferenceTable
import net.atos.zac.admin.model.ReferenceTableValue
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
    var naam: String,

    var isSystemValue: Boolean
)

fun RestReferenceTableValue.toReferenceTableValue(
    referenceTable: ReferenceTable
): ReferenceTableValue = ReferenceTableValue().apply {
    id = this@toReferenceTableValue.id
    name = this@toReferenceTableValue.naam
    isSystemValue = this@toReferenceTableValue.isSystemValue
    this.referenceTable = referenceTable
}
