/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import com.fasterxml.jackson.annotation.JsonFormat
import net.atos.zac.app.identity.model.RestGroup
import net.atos.zac.app.identity.model.RestUser
import net.atos.zac.app.policy.model.RestZaakRechten
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.LocalDate
import java.util.UUID

@NoArgConstructor
@AllOpen
data class RestZaakOverzicht(
    var identificatie: String? = null,

    var toelichting: String? = null,

    var omschrijving: String? = null,

    var uuid: UUID? = null,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    var startdatum: LocalDate? = null,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    var einddatum: LocalDate? = null,

    var zaaktype: String? = null,

    var status: String? = null,

    var behandelaar: RestUser? = null,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    var einddatumGepland: LocalDate? = null,

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    var uiterlijkeEinddatumAfdoening: LocalDate? = null,

    var groep: RestGroup? = null,

    var resultaat: RestZaakResultaat? = null,

    var openstaandeTaken: RestOpenstaandeTaken? = null,

    var rechten: RestZaakRechten? = null
)
