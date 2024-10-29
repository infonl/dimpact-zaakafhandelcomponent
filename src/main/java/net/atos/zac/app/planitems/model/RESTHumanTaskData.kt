/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.planitems.model

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import net.atos.zac.app.identity.model.RestGroup
import net.atos.zac.app.identity.model.RestUser
import java.time.LocalDate

class RESTHumanTaskData {
    var planItemInstanceId: String? = null

    var groep: RestGroup? = null

    var medewerker: @Valid RestUser? = null

    /**
     * The 'final due date' of a task.
     * Note that this fatal date cannot come after the fatal date of the zaak to which this task belongs.
     */
    var fataledatum: LocalDate? = null

    var toelichting: String? = null

    var taakdata: Map<String, String>? = null

    var taakStuurGegevens: @NotNull RESTTaakStuurGegevens? = null
}
