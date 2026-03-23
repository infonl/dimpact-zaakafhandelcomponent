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
    var zaakbeeindigReden: RestZaakbeeindigReden,
    var resultaattype: RestResultaattype
)

fun List<RestZaakbeeindigParameter>.toZaaktypeCompletionParametersList() = map { it.toZaaktypeCompletionParameters() }

fun RestZaakbeeindigParameter.toZaaktypeCompletionParameters() = ZaaktypeCompletionParameters().apply {
    id = this@toZaaktypeCompletionParameters.id
    zaakbeeindigReden = RESTZaakbeeindigRedenConverter.convertRESTZaakbeeindigReden(
        this@toZaaktypeCompletionParameters.zaakbeeindigReden
    )
    resultaattype = this@toZaaktypeCompletionParameters.resultaattype.id
}
