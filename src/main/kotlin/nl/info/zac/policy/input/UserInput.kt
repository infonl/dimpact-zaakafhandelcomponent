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
                    loggedInUser.applicationRolesPerZaaktype.values.flatMap { it }.toSet()
                }
            else -> loggedInUser.roles
        },
        zaaktypen = if (featureFlagPabcIntegration) {
            // This needs to be changed still for the new IAM architecture, probably including existing zaaktype checks
            // in the OPA policy Rego files themselves.
            // This is because with PABC integration enabled the concept of 'being authorised for a zaaktype' no longer exists,
            // since users are always authorised for a zaaktype _for specific application roles_.
            // When there is no zaaktype specified we now simply return `null` indicating that the user is
            // 'authorised for all zaaktypes'.
            if (zaaktype != null) setOf(zaaktype) else null
        } else {
            loggedInUser.geautoriseerdeZaaktypen
        }
    )
}
