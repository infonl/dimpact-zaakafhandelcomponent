/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.taken.model

import jakarta.validation.constraints.NotBlank
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor

@AllOpen
@NoArgConstructor
data class RESTTaakVerdelenGegevens(
    var taken: List<RESTTaakVerdelenTaak>,

    @field:NotBlank
    var groepId: String,

    var behandelaarGebruikersnaam: String? = null,

    var reden: String? = null,

    /**
     * Unique screen event resource ID which can be used
     * to track the progress of the 'assign taken from list' asynchronous job
     * using web sockets.
     */
    var screenEventResourceId: String? = null
)
