/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.identity.model

import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.identity.model.getFullName
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
class RestLoggedInUser(
    username: String,
    naam: String,
    var groupIds: Set<String>? = null,
    var functionalRoles: Set<String>? = null,
    var applicationRoles: Map<String, Set<String>>? = null,
) : RestUser(username, naam)

fun LoggedInUser.toRestLoggedInUser(): RestLoggedInUser =
    RestLoggedInUser(
        username = this.username,
        naam = this.getFullName(),
        groupIds = this.groupIds,
        functionalRoles = this.roles,
        applicationRoles = this.applicationRolesPerZaaktype,
    )
