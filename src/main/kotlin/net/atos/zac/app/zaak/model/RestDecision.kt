/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaak.model

import jakarta.json.bind.annotation.JsonbProperty
import net.atos.client.zgw.brc.model.generated.VervalredenEnum
import net.atos.zac.app.informatieobjecten.model.RestEnkelvoudigInformatieobject
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.net.URI
import java.time.LocalDate
import java.util.UUID

@AllOpen
@NoArgConstructor
data class RestDecision(
    var url: URI,

    var uuid: UUID,

    var identificatie: String? = null,

    var datum: LocalDate? = null,

    var besluittype: RestDecisionType? = null,

    var ingangsdatum: LocalDate? = null,

    var vervaldatum: LocalDate? = null,

    var vervalreden: VervalredenEnum? = null,

    var publicationDate: LocalDate? = null,

    var lastResponseDate: LocalDate? = null,

    @get:JsonbProperty("isIngetrokken")
    var isIngetrokken: Boolean = false,

    var toelichting: String? = null,

    var zaakUuid: UUID? = null,

    var informatieobjecten: List<RestEnkelvoudigInformatieobject>? = null,
)
