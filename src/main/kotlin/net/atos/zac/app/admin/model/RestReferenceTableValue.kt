/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.model

import net.atos.zac.admin.model.ReferenceTable
import net.atos.zac.admin.model.ReferenceTableValue

data class RestReferenceTableValue(
    val id: Long,
    val value: String
)

fun RestReferenceTableValue.toReferenceTableValue(
    referenceTable: ReferenceTable
): ReferenceTableValue = ReferenceTableValue().apply {
    id = this@toReferenceTableValue.id
    name = this@toReferenceTableValue.value
    this.referenceTable = referenceTable
}
