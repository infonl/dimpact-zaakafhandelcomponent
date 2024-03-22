/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.time.LocalDate

@NoArgConstructor
@AllOpen
data class RESTZaakOpschortGegevens(
    var indicatieOpschorting: Boolean,

    var redenOpschorting: String? = null,

    var duurDagen: Long = 0,

    var einddatumGepland: LocalDate? = null,

    var uiterlijkeEinddatumAfdoening: LocalDate? = null
)
