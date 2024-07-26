/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.converter

import net.atos.zac.admin.model.ReferenceTable
import net.atos.zac.admin.model.ReferenceTableValue
import net.atos.zac.app.admin.model.RestReferenceTableValue

fun convertToRestReferenceTableValue(referenceTableValue: ReferenceTableValue) =
    RestReferenceTableValue(
        referenceTableValue.id!!,
        referenceTableValue.name
    )

fun getReferenceTableValueNames(referenceTableValues: List<ReferenceTableValue>) =
    referenceTableValues
        .map(ReferenceTableValue::name)
        .toList()

fun convertToReferenceTableValue(
    referenceTable: ReferenceTable,
    restReferenceTableValue: RestReferenceTableValue
) = ReferenceTableValue().apply {
    this.id = restReferenceTableValue.id
    this.name = restReferenceTableValue.value
    this.referenceTable = referenceTable
}
