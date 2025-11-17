/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.identity.model

import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RestUserAuthorization(
    var groups: Set<String>,
    var functionalRoles: Set<String>,
    var applicationRolesPerZaaktype: Map<String, Set<String>>
)

fun LoggedInUser.toRestUserAuthorization(): RestUserAuthorization =
    RestUserAuthorization(
        groups = this.groupIds,
        functionalRoles = this.roles,
        applicationRolesPerZaaktype = this.applicationRolesPerZaaktype
    )
