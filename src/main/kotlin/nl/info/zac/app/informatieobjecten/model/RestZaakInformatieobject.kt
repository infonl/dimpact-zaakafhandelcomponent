/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.informatieobjecten.model

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.app.policy.model.RestZaakRechten
import nl.info.zac.app.zaak.model.RestZaakStatus
import java.time.LocalDate

data class RestZaakInformatieobject(
    val zaakIdentificatie: String,

    @get:JsonbProperty("isZaakspecifiekGeautoriseerd")
    val isZaakspecifiekGeautoriseerd: Boolean = false,

    val zaakStatus: RestZaakStatus? = null,
    val zaakStartDatum: LocalDate? = null,
    val zaakEinddatumGepland: LocalDate? = null,
    val zaaktypeOmschrijving: String? = null,
    val zaakRechten: RestZaakRechten
)
