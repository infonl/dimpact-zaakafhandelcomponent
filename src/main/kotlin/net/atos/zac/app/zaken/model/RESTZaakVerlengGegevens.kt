/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

import java.time.LocalDate

class RESTZaakVerlengGegevens {
    var redenVerlenging: String? = null

    var duurDagen: Int = 0

    var takenVerlengen: Boolean = false

    var einddatumGepland: LocalDate? = null

    var uiterlijkeEinddatumAfdoening: LocalDate? = null
}
