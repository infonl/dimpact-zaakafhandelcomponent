/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.taken.model

import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.util.UUID

@AllOpen
@NoArgConstructor
data class RESTTaakToekennenGegevens(
    var taakId: String,

    var zaakUuid: UUID,

    var groepId: String,

    var behandelaarId: String? = null,

    var reden: String? = null
)
