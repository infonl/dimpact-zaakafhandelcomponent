/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.signalering.model

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.ZonedDateTime

@AllOpen
@NoArgConstructor
data class RestSignaleringTaskSummary(
    var id: String,
    var naam: String,
    var zaakIdentificatie: String,

    @get:JsonbProperty("isZaakspecifiekGeautoriseerd")
    var isZaakspecifiekGeautoriseerd: Boolean,

    var zaaktypeOmschrijving: String,
    var creatiedatumTijd: ZonedDateTime
)
