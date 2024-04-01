/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.util.UUID

@NoArgConstructor
@AllOpen
data class RESTZakenVerdeelGegevens(
    var uuids: List<UUID>,

    var reden: String? = null,

    var groepId: String? = null,

    var behandelaarGebruikersnaam: String? = null
)
