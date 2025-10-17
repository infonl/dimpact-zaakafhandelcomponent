/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.model

import nl.info.zac.app.policy.model.RestZaakRechten
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.LocalDate

@AllOpen
@NoArgConstructor
data class RestGerelateerdeZaak(
    var relatieType: RelatieType? = null,

    var identificatie: String? = null,

    var zaaktypeOmschrijving: String? = null,

    var statustypeOmschrijving: String? = null,

    var startdatum: LocalDate? = null,

    var rechten: RestZaakRechten? = null,
)
