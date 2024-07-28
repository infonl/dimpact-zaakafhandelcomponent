/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.model

import net.atos.zac.admin.model.ReferenceTable
import net.atos.zac.admin.model.ReferenceTableValue
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
class RestReferenceTableValue(
    var id: Long,
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
