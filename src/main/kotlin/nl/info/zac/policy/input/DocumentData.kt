/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.input

import jakarta.json.bind.annotation.JsonbProperty

data class DocumentData(
    @get:JsonbProperty("definitief")
    val isDefinitief: Boolean = false,

    @get:JsonbProperty("vergrendeld")
    val isVergrendeld: Boolean = false,

    @get:JsonbProperty("ondertekend")
    val isOndertekend: Boolean = false,

    @field:JsonbProperty("vergrendeld_door")
    val vergrendeldDoor: String? = null,

    @field:JsonbProperty("zaaktype")
    val zaaktype: String? = null,

    @get:JsonbProperty("zaak_open")
    val isZaakOpen: Boolean = false,

    @get:JsonbProperty("zaakspecifiekGeautoriseerd")
    val isZaakspecifiekGeautoriseerd: Boolean = false,

    @get:JsonbProperty("loggedInUserIsGeautoriseerdeMedewerker")
    val isLoggedInUserGeautoriseerdeMedewerker: Boolean = false
)
