/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zaak.model

import net.atos.zac.app.policy.model.RestZaakRechten
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
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
