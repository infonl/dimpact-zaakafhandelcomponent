/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

import com.fasterxml.jackson.annotation.JsonFormat
import net.atos.zac.app.identity.model.RESTGroup
import net.atos.zac.app.identity.model.RESTUser
import net.atos.zac.app.policy.model.RESTZaakRechten
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.time.LocalDate
import java.util.UUID

@NoArgConstructor
@AllOpen
data class RESTZaakOverzicht(
    var identificatie: String? = null,

    var toelichting: String? = null,

    var omschrijving: String? = null,

    var uuid: UUID? = null,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
    var startdatum: LocalDate? = null,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
    var einddatum: LocalDate? = null,

    var zaaktype: String? = null,

    var status: String? = null,

    var behandelaar: RESTUser? = null,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
    var einddatumGepland: LocalDate? = null,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd")
    var uiterlijkeEinddatumAfdoening: LocalDate? = null,

    var groep: RESTGroup? = null,

    var resultaat: RESTZaakResultaat? = null,

    var openstaandeTaken: RESTOpenstaandeTaken? = null,

    var rechten: RESTZaakRechten? = null
)
