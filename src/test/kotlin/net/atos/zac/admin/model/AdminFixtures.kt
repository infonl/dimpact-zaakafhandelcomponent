/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.admin.model

import java.util.UUID

fun createReferenceTable(
    id: Long = 1234L,
    code: String = "dummyCode",
    name: String = "dummyReferentieTabel",
    isSystemReferenceTable: Boolean = false,
    values: MutableList<ReferenceTableValue> = mutableListOf(createReferenceTableValue())
) = ReferenceTable().apply {
    this.id = id
    this.code = code
    this.name = name
    this.isSystemReferenceTable = isSystemReferenceTable
    this.values = values
}

fun createReferenceTableValue(
    id: Long = 1234L,
    name: String = "dummyReferentieTabelWaarde",
    sortOrder: Int = 1,
    isSystemValue: Boolean = false
) = ReferenceTableValue().apply {
    this.id = id
    this.name = name
    this.sortOrder = sortOrder
    this.isSystemValue = isSystemValue
}

fun createZaakafhandelParameters(
    id: Long = 1234L,
    zaakTypeUUID: UUID = UUID.randomUUID(),
) =
    ZaakafhandelParameters().apply {
        this.id = id
        this.zaakTypeUUID = zaakTypeUUID
    }
