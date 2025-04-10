/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import jakarta.validation.constraints.NotBlank
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@NoArgConstructor
@AllOpen
data class RESTZakenVerdeelGegevens(
    var uuids: List<UUID>,

    @field:NotBlank
    var groepId: String,

    var behandelaarGebruikersnaam: String? = null,

    var reden: String? = null,

    /**
     * Unique screen event resource ID which can be used
     * to track the progress of the 'assign zaken from list' asynchronous job
     * using web sockets.
     */
    var screenEventResourceId: String? = null
)
