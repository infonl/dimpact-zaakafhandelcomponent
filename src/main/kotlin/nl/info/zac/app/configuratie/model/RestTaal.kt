/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.configuratie.model

import nl.info.zac.configuratie.model.Taal
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
data class RestTaal(
    var id: String,
    var code: String,
    var naam: String,
    var name: String,
    var local: String,
)

fun Taal.toRestTaal() = RestTaal(
    id = this.id.toString(),
    code = this.code,
    naam = this.naam,
    name = this.name,
    local = this.local
)

fun List<Taal>.toRestTalen(): List<RestTaal> = this.map { it.toRestTaal() }
