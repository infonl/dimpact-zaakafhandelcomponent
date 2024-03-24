/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.time.LocalDate

@NoArgConstructor
@AllOpen
data class RESTZaakVerlengGegevens(
    var redenVerlenging: String? = null,

    var duurDagen: Int = 0,

    var takenVerlengen: Boolean,

    var einddatumGepland: LocalDate? = null,

    var uiterlijkeEinddatumAfdoening: LocalDate? = null
)
