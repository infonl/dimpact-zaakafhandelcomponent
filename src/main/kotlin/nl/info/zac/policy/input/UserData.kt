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

    @field:JsonbProperty("overallRoles")
    val overallRoles: Set<String>,

    /**
     * The single zaaktype description for which the logged-in user is authorised
     * for the set of specified application roles.
     * A value of null means that the policy that is being evaluated is not zaaktype-specific.
     */
    @field:JsonbProperty("zaaktypen")
    val zaaktypen: Set<String>? = null,

    @field:JsonbProperty("brpGemeenteCodes")
    val brpGemeenteCodes: Set<String>,
)
