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
class RestReferenceTableUpdate(
    @field:NotBlank
    var naam: String,

    @field:Valid
    var waarden: List<RestReferenceTableValue> = emptyList()
)

fun RestReferenceTableUpdate.updateExistingReferenceTableWithNameAndValues(
    existingReferenceTable: ReferenceTable
) = existingReferenceTable.apply {
    name = this@updateExistingReferenceTableWithNameAndValues.naam
    values = this@updateExistingReferenceTableWithNameAndValues.waarden
        .map { it.toReferenceTableValue(this) }
        .toMutableList()
}
