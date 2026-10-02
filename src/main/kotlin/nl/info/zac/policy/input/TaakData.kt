/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.input

import jakarta.json.bind.annotation.JsonbProperty

data class TaakData(
    @get:JsonbProperty("open")
    val isOpen: Boolean = false,

    @field:JsonbProperty("zaaktype")
    val zaaktype: String? = null,

    @get:JsonbProperty("zaakspecifiekGeautoriseerd")
    val isZaakspecifiekGeautoriseerd: Boolean = false,

    @get:JsonbProperty("loggedInUserIsGeautoriseerdeMedewerker")
    val isLoggedInUserGeautoriseerdeMedewerker: Boolean = false
)
