/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

import java.time.LocalDate

class RESTZaakOpschortGegevens {
    var indicatieOpschorting: Boolean = false

    var redenOpschorting: String? = null

    var duurDagen: Long = 0

    var einddatumGepland: LocalDate? = null

    var uiterlijkeEinddatumAfdoening: LocalDate? = null
}
