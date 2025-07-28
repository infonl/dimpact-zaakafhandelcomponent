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
data class UserData(
    @field:JsonbProperty("id")
    var id: String,

    @field:JsonbProperty("rollen")
    var rollen: Set<String>? = null,

    /**
     * List of zaaktype descriptions for which the logged-in user is authorised.
     * A value of null means that the user is authorised for all zaaktypes.
     */
    @field:JsonbProperty("open")
    var zaaktypen: Set<String>? = null
)
