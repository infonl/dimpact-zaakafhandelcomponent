/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.util.*

@AllOpen
@NoArgConstructor
data class RESTZaakOntkoppelGegevens(
    var zaakUuid: UUID,

    var gekoppeldeZaakIdentificatie: String,

    var relatietype: RelatieType,

    var reden: String
)
