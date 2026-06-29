/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.identity

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.inject.Named
import nl.info.client.pabc.PabcClientService
import nl.info.zac.identity.exception.GroupNotFoundException
import nl.info.zac.identity.exception.UserNotFoundException
import nl.info.zac.identity.exception.UserNotInGroupException
import nl.info.zac.identity.model.Group
import nl.info.zac.identity.model.User
import nl.info.zac.identity.model.ZacApplicationRole
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
    private val keycloakZacRealmResource: RealmResource,

    private val pabcClientService: PabcClientService
) {
    fun listUsers(): List<User> = keycloakZacRealmResource.users()
        .list()
        .map { it.toUser() }
        .sortedBy { it.getFullName() }

    fun listGroups(): List<Group> = keycloakZacRealmResource.groups()
        // retrieve groups with 'full representation' or else the group attributes will not be filled
        .groups("", 0, Integer.MAX_VALUE, false)
        .map { it.toGroup() }
        .sortedBy { it.description }

    fun listActiveGroups(): List<Group> = listGroups().filter { it.active }

    /**
     * Returns the list of active groups that are authorised for the application role 'behandelaar' and
     * the given zaaktype based on the PABC authorisation mappings, using the groups' functional roles in Keycloak.
     * This function requires that the PABC integration feature flag is enabled.
     */
    fun listActiveGroupsForBehandelaarRoleAndZaaktype(zaaktypeDescription: String): List<Group> =
        pabcClientService.getGroupsByApplicationRoleAndZaaktype(
            applicationRole = ZacApplicationRole.BEHANDELAAR.value,
            zaaktypeDescription = zaaktypeDescription
        ).map { it.toGroup() }.filter { it.active }

    /**
     * Returns the intersection of active groups that are authorised for the application role 'behandelaar'
     * across all given zaaktype descriptions, based on the PABC authorisation mappings.
     * Returns an empty list when no group is authorised for all provided zaaktypes.
     * This function requires that the PABC integration feature flag is enabled.
     */
    fun listActiveGroupsForBehandelaarRoleAndZaaktypes(zaaktypeDescriptions: List<String>): List<Group> {
        if (zaaktypeDescriptions.isEmpty()) return emptyList()
        val groupsForFirstZaaktype = listActiveGroupsForBehandelaarRoleAndZaaktype(zaaktypeDescriptions.first())
        // only intersect on group names because the group name is the only unique identifier of a group
        val commonGroupNames = zaaktypeDescriptions.drop(1).fold(groupsForFirstZaaktype.map { it.name }.toSet()) {
                commonNames, zaaktypeDescription ->
            if (commonNames.isEmpty()) return@fold commonNames
            commonNames intersect listActiveGroupsForBehandelaarRoleAndZaaktype(zaaktypeDescription).map { it.name }.toSet()
        }
        return groupsForFirstZaaktype
            .filter { it.name in commonGroupNames }
            .distinctBy { it.name }
            .sortedBy { it.description }
    }

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

    fun isUserInGroup(userId: String, groupId: String) =
        listGroupNamesForUser(userId).contains(groupId)

    fun validateIfUserIsInGroup(userId: String, groupId: String) {
        if (!isUserInGroup(userId, groupId)) {
            throw UserNotInGroupException()
        }
    }
}
