/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.task.model

import net.atos.zac.app.formulieren.model.RESTFormulierDefinitie
import net.atos.zac.app.identity.model.RESTGroup
import net.atos.zac.app.identity.model.RESTUser
import net.atos.zac.app.policy.model.RESTTaakRechten
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

@AllOpen
@NoArgConstructor
data class RestTask(
    var id: String? = null,

    var naam: String? = null,

    var toelichting: String? = null,

    var creatiedatumTijd: ZonedDateTime? = null,

    // Datum waarop de taak is toegekend aan een behandelaar
    var toekenningsdatumTijd: ZonedDateTime? = null,

    var fataledatum: LocalDate? = null,

    var behandelaar: RESTUser? = null,

    var groep: RESTGroup? = null,

    var zaakUuid: UUID? = null,

    var zaakIdentificatie: String? = null,

    var zaaktypeOmschrijving: String? = null,

    var status: TaakStatus? = null,

    // Identificatie van een vooraf gecodeerde combinatie van taak start en afhandel formulieren.
    // Deze worden enkel gebruikt door taken welke handmatig worden gestart vanuit een CMMN model
    var formulierDefinitieId: String? = null,

    // Definitie van een via de user interface gebouwd formulier.
    // Deze worden enkel gebruikt voor het afhandelen van taken welke automatische worden gestart vanuit een BPMN proces
    var formulierDefinitie: RESTFormulierDefinitie? = null,

    // needs to be mutable for now unfortunately because this data can be changed dynamically
    var tabellen: MutableMap<String, List<String>>,

    // needs to be mutable for now unfortunately because this data can be changed dynamically
    var taakdata: MutableMap<String, String>?,

    var taakinformatie: Map<String, String>? = null,

    var taakdocumenten: List<UUID>? = null,

    var rechten: RESTTaakRechten? = null
)
