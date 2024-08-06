/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaak.model

import jakarta.json.bind.annotation.JsonbProperty
import net.atos.client.zgw.brc.model.generated.VervalredenEnum
import net.atos.zac.app.informatieobjecten.model.RESTEnkelvoudigInformatieobject
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.net.URI
import java.time.LocalDate
import java.util.UUID

@AllOpen
@NoArgConstructor
data class RestBesluit(
    var url: URI,

    var uuid: UUID,

    var identificatie: String? = null,

    var datum: LocalDate? = null,

    var besluittype: RestBesluittype? = null,

    var ingangsdatum: LocalDate? = null,

    var vervaldatum: LocalDate? = null,

    var vervalreden: VervalredenEnum? = null,

    @field:JsonbProperty("isIngetrokken")
    var isIngetrokken: Boolean = false,

    var toelichting: String? = null,

    var zaakUuid: UUID? = null,

    var informatieobjecten: List<RESTEnkelvoudigInformatieobject>? = null,
)

// fun Besluit.toRestBesluit() = RestBesluit(
//    uuid = UriUtil.uuidFromURI(this.url),
//    besluittype = restBesluittypeConverter.convertToRESTBesluittype(this.besluittype),
//    datum = this.datum,
//    identificatie = this.identificatie,
//    url = this.url,
//    toelichting = this.toelichting,
//    ingangsdatum = this.ingangsdatum,
//    vervaldatum = this.vervaldatum,
//    vervalreden = this.vervalreden,
//    isIngetrokken = this.vervaldatum != null && (
//        this.vervalreden == VervalredenEnum.INGETROKKEN_BELANGHEBBENDE ||
//            this.vervalreden == VervalredenEnum.INGETROKKEN_OVERHEID
//        ),
//    informatieobjecten = restInformatieobjectConverter.convertInformatieobjectenToREST(
//        listBesluitInformatieobjecten(this)
//    )
// )
