/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.admin.model

import java.util.UUID

fun createHumanTaskParameters(
    id: Long = 1234L,
    zaakafhandelParameters: ZaakafhandelParameters = createZaakafhandelParameters(),
    isActief: Boolean = true
) = HumanTaskParameters().apply {
    this.id = id
    this.zaakafhandelParameters = zaakafhandelParameters
    this.isActief = isActief
}

fun createHumanTaskReferentieTabel(
    id: Long = 1234L,
    referenceTable: ReferenceTable = createReferenceTable(),
    humanTaskParameters: HumanTaskParameters = createHumanTaskParameters(),
    field: String = "dummyField",
) = HumanTaskReferentieTabel().apply {
    this.id = id
    this.tabel = referenceTable
    this.humantask = humanTaskParameters
    this.veld = field
}

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
    domein: String = "dummyDomein",
    zaakTypeUUID: UUID = UUID.randomUUID(),
) =
    ZaakafhandelParameters().apply {
        this.id = id
        this.domein = domein
        this.zaakTypeUUID = zaakTypeUUID
    }
