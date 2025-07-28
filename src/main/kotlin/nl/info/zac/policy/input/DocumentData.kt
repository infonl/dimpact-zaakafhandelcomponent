/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.input

import jakarta.json.bind.annotation.JsonbProperty

data class DocumentData(
    @field:JsonbProperty("definitief")
    val definitief: Boolean = false,

    @field:JsonbProperty("vergrendeld")
    val vergrendeld: Boolean = false,

    @field:JsonbProperty("ondertekend")
    val ondertekend: Boolean = false,

    @field:JsonbProperty("vergrendeld_door")
    val vergrendeldDoor: String? = null,

    @field:JsonbProperty("zaaktype")
    val zaaktype: String? = null,

    @field:JsonbProperty("zaak_open")
    val zaakOpen: Boolean = false
)
