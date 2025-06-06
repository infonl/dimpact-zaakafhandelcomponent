/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.input

import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
class UserInput {
    lateinit var user: UserData

    constructor(
        loggedInUser: LoggedInUser
    ) {
        user = UserData(
            id = loggedInUser.id,
            rollen = loggedInUser.roles,
            zaaktypen = if (loggedInUser.isAuthorisedForAllZaaktypen()) {
                null
            } else {
                loggedInUser.geautoriseerdeZaaktypen
            }
        )
    }
}
