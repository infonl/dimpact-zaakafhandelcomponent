/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.input

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.authentication.LoggedInUser

open class UserInput(
    loggedInUser: LoggedInUser,
    zaaktype: String? = null,
    featureFlagPabcIntegration: Boolean = false
) {
    @field:JsonbProperty("user")
    val user = UserData(
        id = loggedInUser.id,
        rollen = when {
            featureFlagPabcIntegration ->
                if (zaaktype != null) {
                    loggedInUser.applicationRolesPerZaaktype[zaaktype].orEmpty()
                } else {
                    // No zaaktype is specified so this concerns a policy check that is zaaktype-independent.
                    // In that case the authorized application roles are those for which at least one zaaktype is authorized.
                    loggedInUser.applicationRolesPerZaaktype.values.flatten().toSet()
                }
            else -> loggedInUser.roles
        },
        zaaktypen = if (featureFlagPabcIntegration) {
            // In the new IAM architecture this can only ever be a single zaaktype, since zaaktype-specific policy
            // checks are always evaluated in the context of a single specific zaaktype.
            zaaktype?.let { setOf(it) }
        } else {
            // In the old IAM architecture this is the list of zaaktypen the user is authorized for, regardless of roles.
            loggedInUser.geautoriseerdeZaaktypen
        }
    )
}
