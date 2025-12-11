/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.identity

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.inject.Named
import net.atos.zac.admin.ZaaktypeCmmnConfigurationService
import nl.info.client.pabc.PabcClientService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.configuratie.ConfiguratieService
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
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.keycloak.admin.client.resource.RealmResource
import java.util.UUID
import kotlin.collections.filter

@AllOpen
@NoArgConstructor
@ApplicationScoped
@Suppress("TooManyFunctions")
class IdentityService @Inject constructor(
    @Named("keycloakZacRealmResource")
    private val keycloakZacRealmResource: RealmResource,
    private val zaaktypeCmmnConfigurationService: ZaaktypeCmmnConfigurationService,
    private val configuratieService: ConfiguratieService,

    @ConfigProperty(name = "AUTH_RESOURCE")
    private val zacKeycloakClientId: String,
    private val pabcClientService: PabcClientService,
    private val ztcClientService: ZtcClientService
) {
    fun listUsers(): List<User> = keycloakZacRealmResource.users()
        .list()
        .map { it.toUser() }
        .sortedBy { it.getFullName() }

    fun listGroups(): List<Group> = keycloakZacRealmResource.groups()
        // retrieve groups with 'full representation' or else the group attributes will not be filled
        .groups("", 0, Integer.MAX_VALUE, false)
        .map { it.toGroup(zacKeycloakClientId) }
        .sortedBy { it.description }

    /**
     * New IAM: TODO
     * Old IAM: With the ZAC PABC feature flag off: returns the list of groups that have access to the given zaaktype UUID
     * based on the ZAC domain roles (if any) of this group and the domain (if any) configured in the zaakafhandelparameters
     * for this zaaktype.
     * With the ZAC PABC feature flag on: returns all available groups, because group authorisation (for zaaktypes) is not yet
     * supported in the new IAM architecture. This will be implemented in a future release of the PABC and ZAC.
     */
    fun listGroupsForZaaktypeUuid(zaaktypeUuid: UUID): List<Group> {
        return if (configuratieService.featureFlagPabcIntegration()) {
            // TODO: get the frontend to also pass on the zaaktype description in th endpoint?
            val zaaktype = ztcClientService.readZaaktype(zaaktypeUuid)
            pabcClientService.getGroupsByApplicationRoleAndZaaktype(
                applicationRole = ZacApplicationRole.BEHANDELAAR.value,
                zaaktypeDescription = zaaktype.omschrijvingGeneriek
            ).map { it.toGroup() }
        } else {
            // retrieve groups with 'full representation' or else the group attributes will not be filled
            val groups = keycloakZacRealmResource.groups()
                .groups("", 0, Integer.MAX_VALUE, false)
                .map { it.toGroup(zacKeycloakClientId) }
            // only filter groups on domain authorisation when PABC integration is disabled
            val domein = zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaaktypeUuid).domein
            groups.filter {
                (domein == null || domein == ZacApplicationRole.DOMEIN_ELK_ZAAKTYPE.value) ||
                    it.zacClientRoles.contains(domein)
            }
        }
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
        .firstOrNull()?.toGroup(zacKeycloakClientId)
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
