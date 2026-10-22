/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import nl.info.zac.admin.model.ZaakbeeindigReden
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RestZaakbeeindigReden(
    var id: String,
    var naam: String
)

fun ZaakbeeindigReden.toRestZaakbeeindigReden() = RestZaakbeeindigReden(
    id = checkNotNull(id) { "Zaakbeeindigreden '$naam' has no id" }.toString(),
    naam = checkNotNull(naam) { "Zaakbeeindigreden with id '$id' has no naam" }
)

fun RestZaakbeeindigReden.toZaakbeeindigReden() = ZaakbeeindigReden().apply {
    id = this@toZaakbeeindigReden.id.toLong()
    naam = this@toZaakbeeindigReden.naam
}
