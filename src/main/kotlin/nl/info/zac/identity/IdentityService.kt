/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.identity

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.inject.Named
import nl.info.zac.identity.exception.GroupNotFoundException
import nl.info.zac.identity.exception.UserNotFoundException
import nl.info.zac.identity.exception.UserNotInGroupException
import nl.info.zac.identity.model.Group
import nl.info.zac.identity.model.User
import nl.info.zac.identity.model.getFullName
import nl.info.zac.identity.model.toGroup
import nl.info.zac.identity.model.toUser
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.keycloak.admin.client.resource.RealmResource

@AllOpen
@NoArgConstructor
@ApplicationScoped
@Suppress("TooManyFunctions")
class IdentityService @Inject constructor(
    @Named("keycloakZacRealmResource")
    private val keycloakZacRealmResource: RealmResource
) {
    fun listUsers(): List<User> = keycloakZacRealmResource.users()
        .list()
        .map { it.toUser() }
        .sortedBy { it.getFullName() }

    fun listGroups(): List<Group> = keycloakZacRealmResource.groups()
        // retrieve groups with 'full representation' or else the group attributes will not be filled
        .groups("", 0, Integer.MAX_VALUE, false)
        .map { it.toGroup() }
        .sortedBy { it.name }

    fun readUser(userId: String): User = keycloakZacRealmResource.users()
        .searchByUsername(userId, true)
        .map { it.toUser() }.firstOrNull()
        // is this fallback really needed? better to return null or throw a custom exception
        ?: User(userId)

    fun readGroup(groupId: String): Group = keycloakZacRealmResource.groups()
        // retrieve groups with 'full representation' or else the group attributes will not be filled
        .groups(groupId, true, 0, 1, false)
        .firstOrNull()?.toGroup()
        // is this fallback really needed? better to return null or throw a custom exception
        ?: Group(groupId)

    fun listUsersInGroup(groupId: String): List<User> {
        val keycloakGroupId = keycloakZacRealmResource.groups()
            .groups(groupId, true, 0, 1, true)
            .firstOrNull()?.id
            ?: throw GroupNotFoundException()
        return keycloakZacRealmResource.groups()
            .group(keycloakGroupId)
            .members()
            .map { it.toUser() }
            .sortedBy { it.getFullName() }
    }

    fun listGroupNamesForUser(userId: String): List<String> {
        val keycloakUserId = keycloakZacRealmResource.users()
            .searchByUsername(userId, true).firstOrNull()?.id
            ?: throw UserNotFoundException()
        return keycloakZacRealmResource.users()
            .get(keycloakUserId)
            .groups()
            .map { it.name }
    }

    fun checkIfUserIsInGroup(userId: String, groupId: String) {
        if (!listGroupNamesForUser(userId).contains(groupId)) {
            throw UserNotInGroupException()
        }
    }
}
