/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.identity.model

import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.identity.model.getFullName
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
class RestLoggedInUser(
    id: String,
    naam: String,
    var groupIds: Set<String>? = null,
) : RestUser(id, naam)

fun LoggedInUser.toRestLoggedInUser(): RestLoggedInUser =
    RestLoggedInUser(
        id = this.id,
        naam = this.getFullName(),
        groupIds = this.groupIds
    )
