/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.converter

import net.atos.zac.admin.model.ReferenceTable
import net.atos.zac.admin.model.ReferenceTableValue
import net.atos.zac.app.admin.model.RestReferenceTable
import net.atos.zac.app.admin.model.RestReferenceTableValue
import java.util.Objects

fun convertToRestReferenceTable(referenceTable: ReferenceTable, inclusiefWaarden: Boolean): RestReferenceTable {
    var values: List<RestReferenceTableValue>? = emptyList()
    if (inclusiefWaarden) {
        values = referenceTable.values.stream()
            .map { referenceTableValue: ReferenceTableValue? ->
                RestReferenceValueConverter.convert(
                    referenceTableValue
                )
            }
            .toList()
    }
    return RestReferenceTable(
        referenceTable.id!!,
        referenceTable.code,
        referenceTable.name,
        referenceTable.isSystemReferenceTable,
        referenceTable.values.size,
        values!!
    )
}

fun convertToReferenceTable(
    restReferenceTable: RestReferenceTable,
    referenceTable: ReferenceTable = ReferenceTable()
): ReferenceTable {
    referenceTable.code = restReferenceTable.code
    referenceTable.name = restReferenceTable.name
    referenceTable.values = Objects.requireNonNull(restReferenceTable.values)
        .stream()
        .map { referenceTableValues: RestReferenceTableValue? ->
            RestReferenceValueConverter.convert(
                referenceTable,
                referenceTableValues
            )
        }
        .toList()

    return referenceTable
}
