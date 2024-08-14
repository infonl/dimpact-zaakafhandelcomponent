/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.identity.converter

import jakarta.inject.Inject
import net.atos.zac.app.identity.model.RestLoggedInUser
import net.atos.zac.app.identity.model.RestUser
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.identity.IdentityService
import net.atos.zac.identity.model.User
import net.atos.zac.identity.model.getFullNameResolved
import nl.lifely.zac.util.NoArgConstructor

@NoArgConstructor
class RestUserConverter @Inject constructor(
    private val identityService: IdentityService
) {
    fun convertUsers(users: List<User>): List<RestUser> =
        users.map { this.convertUser(it) }

    fun convertUserIds(userIds: List<String>): List<RestUser> =
        userIds.map { this.convertUserId(it) }

    fun convertUser(user: User): RestUser =
        RestUser(
            user.id,
            user.getFullNameResolved()
        )

    fun convertLoggedInUser(user: LoggedInUser): RestLoggedInUser {
        val restUser = RestLoggedInUser(
            id = user.id,
            naam = user.getFullNameResolved(),
            groupIds = user.groupIds
        )
        return restUser
    }

    fun convertUserId(userId: String): RestUser =
        convertUser(identityService.readUser(userId))
}
