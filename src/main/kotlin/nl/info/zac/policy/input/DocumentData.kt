/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.input

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class DocumentData(
    var definitief: Boolean = false,

    var vergrendeld: Boolean = false,

    var ondertekend: Boolean = false,

    @field:JsonbProperty("vergrendeld_door")
    var vergrendeldDoor: String? = null,

    var zaaktype: String? = null,

    @field:JsonbProperty("zaak_open")
    var zaakOpen: Boolean = false
)
