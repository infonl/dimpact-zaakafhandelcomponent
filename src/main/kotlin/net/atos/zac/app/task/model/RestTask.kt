/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.task.model

import jakarta.json.JsonObject
import net.atos.zac.app.formulieren.model.RESTFormulierDefinitie
import net.atos.zac.app.identity.model.RestGroup
import net.atos.zac.app.identity.model.RestUser
import net.atos.zac.app.policy.model.RestTaakRechten
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
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

    /**
     * Date and time when the task was assigned to the assignee
     */
    var toekenningsdatumTijd: ZonedDateTime? = null,

    var fataledatum: LocalDate? = null,

    var behandelaar: RestUser? = null,

    var groep: RestGroup? = null,

    var zaakUuid: UUID,

    var zaakIdentificatie: String,

    var zaaktypeOmschrijving: String? = null,

    var status: TaakStatus? = null,

    /**
     * Identificatie van een vooraf gecodeerde combinatie van taak start en afhandel formulieren.
     * Deze worden enkel gebruikt door taken welke handmatig worden gestart vanuit een CMMN model
     */
    var formulierDefinitieId: String? = null,

    /**
     * Definitie van een via de user interface gebouwd formulier.
     * Deze worden enkel gebruikt voor het afhandelen van taken welke automatische worden gestart vanuit een BPMN proces.
     */
    var formulierDefinitie: RESTFormulierDefinitie? = null,

    var formioFormulier: JsonObject? = null,

    // needs to be mutable for now unfortunately because this data can be changed dynamically
    var tabellen: MutableMap<String, List<String>>,

    // needs to be mutable for now unfortunately because this data can be changed dynamically
    var taakdata: MutableMap<String, Any>?,

    var taakinformatie: Map<String, String>? = null,

    var taakdocumenten: List<UUID>? = null,

    var rechten: RestTaakRechten? = null
)
