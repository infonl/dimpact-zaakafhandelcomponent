/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.admin.model

import java.util.UUID

fun createReferenceTable(
    id: Long = 1234L,
    code: String = "dummyCode",
    naam: String = "dummyReferentieTabel"
) =
    ReferenceTable().apply {
        this.id = id
        this.code = code
        this.name = naam
    }

fun createReferenceTableValue(
    id: Long = 1234L,
    naam: String = "dummyReferentieTabelWaarde",
    volgorde: Int = 1
) = ReferenceTableValue().apply {
    this.id = id
    this.name = naam
    this.sortOrder = volgorde
}

fun createZaakafhandelParameters(
    id: Long = 1234L,
    zaakTypeUUID: UUID = UUID.randomUUID(),
) =
    ZaakafhandelParameters().apply {
        this.id = id
        this.zaakTypeUUID = zaakTypeUUID
    }
