/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

import net.atos.zac.app.klanten.model.klant.IdentificatieType
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.util.UUID

@AllOpen
@NoArgConstructor
data class RESTZaakBetrokkeneGegevens(
    var zaakUUID: UUID,
    var roltypeUUID: UUID,
    var roltoelichting: String,
    var betrokkeneIdentificatieType: IdentificatieType,
    var betrokkeneIdentificatie: String
)
