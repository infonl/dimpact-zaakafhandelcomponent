/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.input

import jakarta.json.bind.annotation.JsonbProperty

data class UserData(
    @field:JsonbProperty("id")
    val id: String,

    @field:JsonbProperty("rollen")
    val rollen: Set<String>,

    /**
     * New IAM architecture: the single zaaktype description for which the logged-in user is authorised,
     * _for the set of specified application roles_.
     * A value of null means that the policy that is being evaluated is not zaaktype-specific.
     *
     * Old IAM architecture: list of zaaktype descriptions for which the logged-in user is authorised, regardless of roles.
     * A value of null means that the user is authorised for all zaaktypes.
     */
    @field:JsonbProperty("zaaktypen")
    val zaaktypen: Set<String>? = null
)
