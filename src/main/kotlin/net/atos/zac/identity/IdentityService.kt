/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.identity

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.inject.Named
import net.atos.zac.identity.exception.IdentityRuntimeException
import net.atos.zac.identity.model.Group
import net.atos.zac.identity.model.User
import net.atos.zac.identity.model.toGroup
import net.atos.zac.identity.model.toUser
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
        .sortedBy { it.fullName }

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
            // better throw a custom 'GroupNotFoundException' here
            .firstOrNull()?.id ?: throw IdentityRuntimeException("Group with name '$groupId' not found in Keycloak")
        return keycloakZacRealmResource.groups()
            .group(keycloakGroupId)
            .members()
            .map { it.toUser() }
            .sortedBy { it.fullName }
    }
}
