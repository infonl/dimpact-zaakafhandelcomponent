/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin

import net.atos.zac.app.admin.model.RestReferenceTable
import net.atos.zac.app.admin.model.RestReferenceTableUpdate
import net.atos.zac.app.admin.model.RestReferenceTableValue

@Suppress("LongParameterList")
fun createRestReferenceTable(
    id: Long = 1L,
    code: String = "dummyCode",
    naam: String = "dummyName",
    systeem: Boolean = false,
    aantalWaarden: Int = 2,
    waarden: List<RestReferenceTableValue> = listOf(
        createRestReferenceTableValue(),
        createRestReferenceTableValue()
    )
) = RestReferenceTable(
    id = id,
    code = code,
    naam = naam,
    systeem = systeem,
    aantalWaarden = aantalWaarden,
    waarden = waarden
)

fun createRestReferenceTableUpdate(
    naam: String = "dummyUpdatedName",
    waarden: List<RestReferenceTableValue> = listOf(
        createRestReferenceTableValue(),
        createRestReferenceTableValue()
    )
) = RestReferenceTableUpdate(
    naam = naam,
    waarden = waarden
)

fun createRestReferenceTableValue(
    id: Long = 1234L,
    name: String = "dummyWaarde1",
    isSystemValue: Boolean = false
) = RestReferenceTableValue(
    id = id,
    naam = name,
    isSystemValue = isSystemValue
)
