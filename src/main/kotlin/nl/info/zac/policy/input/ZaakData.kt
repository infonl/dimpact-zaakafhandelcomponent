/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.input

import jakarta.json.bind.annotation.JsonbProperty

data class ZaakData(
    @get:JsonbProperty("open")
    val isOpen: Boolean,

    @field:JsonbProperty("zaaktype")
    val zaaktype: String,

    @get:JsonbProperty("opgeschort")
    val isOpgeschort: Boolean,

    @get:JsonbProperty("verlengd")
    val isVerlengd: Boolean,

    @get:JsonbProperty("intake")
    val isIntake: Boolean?,

    @get:JsonbProperty("besloten")
    val isBesloten: Boolean?,

    @get:JsonbProperty("heropend")
    val isHeropend: Boolean?,

    @get:JsonbProperty("brondatumBepaald")
    val isBrondatumBepaald: Boolean?,

    @get:JsonbProperty("zaakspecifiekGeautoriseerd")
    val isZaakspecifiekGeautoriseerd: Boolean,

    @get:JsonbProperty("loggedInUserIsGeautoriseerdeMedewerker")
    val isLoggedInUserGeautoriseerdeMedewerker: Boolean = false,
)
