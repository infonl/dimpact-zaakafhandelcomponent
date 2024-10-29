/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.planitems.model

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import net.atos.zac.app.identity.model.RestGroup
import net.atos.zac.app.identity.model.RestUser
import nl.lifely.zac.util.NoArgConstructor
import java.time.LocalDate

@NoArgConstructor
data class RESTHumanTaskData(
    var planItemInstanceId: String? = null,

    @field:NotNull
    var groep: RestGroup,

    @field:Valid
    var medewerker: RestUser? = null,

    /**
     * The 'final due date' of a task.
     * Note that this fatal date cannot come after the fatal date of the zaak to which this task belongs.
     */
    var fataledatum: LocalDate? = null,

    var toelichting: String? = null,

    var taakdata: Map<String, String>? = null,

    @field:NotNull
    var taakStuurGegevens: RESTTaakStuurGegevens
)
