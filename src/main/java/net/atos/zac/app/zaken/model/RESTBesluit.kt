/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

import net.atos.client.zgw.brc.model.generated.Besluit.VervalredenEnum
import net.atos.zac.app.informatieobjecten.model.RESTEnkelvoudigInformatieobject
import java.net.URI
import java.time.LocalDate
import java.util.*

data class RESTBesluit(
    val url: URI,

    var uuid: UUID,

    val identificatie: String? = null,

    val datum: LocalDate? = null,

    val besluittype: RESTBesluittype? = null,

    val ingangsdatum: LocalDate? = null,

    val vervaldatum: LocalDate? = null,

    val vervalreden: VervalredenEnum? = null,

    val isIngetrokken: Boolean = false,

    val toelichting: String? = null,

    val zaakUuid: UUID? = null,

    val informatieobjecten: List<RESTEnkelvoudigInformatieobject>? = null,
)
