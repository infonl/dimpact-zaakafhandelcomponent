/*
 * SPDX-FileCopyrightText: 2023 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import net.atos.zac.app.admin.converter.RESTZaakbeeindigRedenConverter
import net.atos.zac.app.admin.model.RESTZaakbeeindigReden
import nl.info.zac.admin.model.ZaaktypeCompletionParameters
import nl.info.zac.app.zaak.model.RestResultaattype
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RestZaakbeeindigParameter(
    val id: Long? = null,
    // TODO: both fields need to be nullable because the frontend sometimes only sets one of them,
    // I guess because there is a flow in this where first one is set and later the other?
    val zaakbeeindigReden: RESTZaakbeeindigReden? = null,
    val resultaattype: RestResultaattype? = null
)

fun List<RestZaakbeeindigParameter>.toRestZaakbeeindigParameters() = map { it.toRestZaakbeeindigParameter() }

fun RestZaakbeeindigParameter.toRestZaakbeeindigParameter() = ZaaktypeCompletionParameters().apply {
    id = this@toRestZaakbeeindigParameter.id
    zaakbeeindigReden = RESTZaakbeeindigRedenConverter.convertRESTZaakbeeindigReden(
        this@toRestZaakbeeindigParameter.zaakbeeindigReden
    )
    // TODO: handle null cases properly; preferably make resultaattype non-nullable
    resultaattype = this@toRestZaakbeeindigParameter.resultaattype!!.id
}
