/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaken.model

import net.atos.zac.app.policy.model.RESTZaakRechten
import java.time.LocalDate

class RESTGerelateerdeZaak {
    var relatieType: RelatieType? = null

    var identificatie: String? = null

    var zaaktypeOmschrijving: String? = null

    var statustypeOmschrijving: String? = null

    var startdatum: LocalDate? = null

    var rechten: RESTZaakRechten? = null
}
