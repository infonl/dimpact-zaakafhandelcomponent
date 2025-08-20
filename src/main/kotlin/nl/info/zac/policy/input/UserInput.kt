/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.input

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.authentication.LoggedInUser

open class UserInput(
    loggedInUser: LoggedInUser,
    zaaktype: String? = null
) {
    @field:JsonbProperty("user")
    val user = UserData(
        id = loggedInUser.id,
        rollen = when {
            zaaktype != null -> loggedInUser.applicationRolesPerZaaktype[zaaktype].orEmpty()
            else -> loggedInUser.roles
        },
        zaaktypen = when {
            zaaktype != null -> setOf(zaaktype)
            loggedInUser.isAuthorisedForAllZaaktypen() -> null else -> loggedInUser.geautoriseerdeZaaktypen
        }
    )
}
