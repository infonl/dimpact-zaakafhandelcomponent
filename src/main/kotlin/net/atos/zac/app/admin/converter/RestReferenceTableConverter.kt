/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.converter

import net.atos.zac.admin.model.ReferenceTable
import net.atos.zac.admin.model.toRestReferenceTableValue
import net.atos.zac.app.admin.model.RestReferenceTable
import net.atos.zac.app.admin.model.RestReferenceTableValue
import net.atos.zac.app.admin.model.toReferenceTableValue

fun convertToRestReferenceTable(referenceTable: ReferenceTable, inclusiefWaarden: Boolean): RestReferenceTable {
    var restReferenceTableValues: List<RestReferenceTableValue> = emptyList()
    if (inclusiefWaarden) {
        restReferenceTableValues = referenceTable.values.stream()
            .map { it.toRestReferenceTableValue() }
            .toList()
    }
    return RestReferenceTable(
        referenceTable.id!!,
        referenceTable.code,
        referenceTable.name,
        referenceTable.isSystemReferenceTable,
        referenceTable.values.size,
        restReferenceTableValues
    )
}

fun convertToReferenceTable(
    restReferenceTable: RestReferenceTable,
    existingReferenceTable: ReferenceTable? = null
): ReferenceTable {
    val referenceTable = existingReferenceTable ?: ReferenceTable()
    return referenceTable.apply {
        this.code = restReferenceTable.code
        this.name = restReferenceTable.name
        this.values = restReferenceTable.values
            .map { referenceTableValue ->
                referenceTableValue.toReferenceTableValue(
                    this
                )
            }
            .toMutableList()
    }
}
