/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.input

import jakarta.json.bind.annotation.JsonbProperty
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
open class UserInput(
    loggedInUser: LoggedInUser
) {
    @field:JsonbProperty("user")
    val user = UserData(
        id = loggedInUser.id,
        rollen = loggedInUser.roles,
        zaaktypen = if (loggedInUser.isAuthorisedForAllZaaktypen()) null else loggedInUser.geautoriseerdeZaaktypen
    )
}
