/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
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
