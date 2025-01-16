/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zoeken.model

import net.atos.zac.app.policy.model.RestTaakRechten
import net.atos.zac.app.task.model.TaakStatus
import nl.info.zac.util.NoArgConstructor
import java.time.LocalDate

@NoArgConstructor
data class RestTaakZoekObject(
    var naam: String? = null,

    var toelichting: String? = null,

    var status: TaakStatus? = null,

    var zaakUuid: String? = null,

    var zaakIdentificatie: String? = null,

    var zaakOmschrijving: String? = null,

    var zaakToelichting: String? = null,

    var zaaktypeUuid: String? = null,

    var zaaktypeIdentificatie: String? = null,

    var zaaktypeOmschrijving: String? = null,

    var creatiedatum: LocalDate? = null,

    var toekenningsdatum: LocalDate? = null,

    var fataledatum: LocalDate? = null,

    var groepID: String? = null,

    var groepNaam: String? = null,

    var behandelaarNaam: String? = null,

    var behandelaarGebruikersnaam: String? = null,

    var taakData: MutableList<String?>? = null,

    var taakInformatie: MutableList<String?>? = null,

    var rechten: RestTaakRechten? = null
) : AbstractRestZoekObject()
