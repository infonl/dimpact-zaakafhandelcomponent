/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import net.atos.zac.app.admin.converter.RESTZaakbeeindigRedenConverter
import net.atos.zac.app.admin.model.RestZaakbeeindigReden
import nl.info.zac.admin.model.ZaaktypeCompletionParameters
import nl.info.zac.app.zaak.model.RestResultaattype
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RestZaakbeeindigParameter(
    // id is nullable to allow creation of new parameters without specifying an id
    var id: Long? = null,
    // unfortunately, both zaakbeeindigReden and resultaattype need to nullable currently,
    // because the frontend only sets one of them at a time
    var zaakbeeindigReden: RestZaakbeeindigReden? = null,
    var resultaattype: RestResultaattype? = null
)

fun List<RestZaakbeeindigParameter>.toZaaktypeCompletionParametersList() = map { it.toZaaktypeCompletionParameters() }

fun RestZaakbeeindigParameter.toZaaktypeCompletionParameters() = ZaaktypeCompletionParameters().apply {
    id = this@toZaaktypeCompletionParameters.id
    zaakbeeindigReden = checkNotNull(this@toZaaktypeCompletionParameters.zaakbeeindigReden) {
        "zaakbeeindigReden cannot be null"
    }.let(RESTZaakbeeindigRedenConverter::convertRESTZaakbeeindigReden)
    resultaattype = checkNotNull(this@toZaaktypeCompletionParameters.resultaattype) {
        "resultaattype cannot be null"
    }.id
}
