/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.identity.model

import nl.info.zac.authentication.UserPrincipalFilter.Companion.ROL_DOMEIN_ELK_ZAAKTYPE
import org.keycloak.representations.idm.GroupRepresentation

data class Group(
    val id: String,

    val name: String,

    val email: String? = null,

    val zacClientRoles: List<String> = emptyList()
) {
    /**
     * Constructor for creating a group not known in the identity system.
     *
     * @param id ID of the group which is unknown
     */
    constructor(id: String) : this(
        id = id,
        name = id
    )
}

/**
 * Converts a [GroupRepresentation] to a [Group], mapping the Keycloak
 * group name, group description, and Keycloak ZAC client roles.
 * Somewhat confusingly, we map the Keycloak group name to our group id,
 * and the Keycloak group description attribute (if any) to our group name.
 * The Keycloak group id is a UUID and is not of interest to us.
 * The Keycloak group name is the unique group name in the ZAC Keycloak realm,
 * and we treat this as the group id in our system.
 *
 * @param keycloakClientId The client ID of the Keycloak client.
 * @return A [Group] object representing the group.
 */
fun GroupRepresentation.toGroup(keycloakClientId: String): Group =
    Group(
        id = name,
        name = attributes?.get("description")?.singleOrNull() ?: name,
        email = attributes?.get("email")?.singleOrNull(),
        zacClientRoles = clientRoles[keycloakClientId].orEmpty()
    )

/**
 * Checks if the group has access to the specified domain.
 *
 * Access is granted if any of these conditions are met:
 * - The domain parameter is null
 * - The domain equals the default domain [ROL_DOMEIN_ELK_ZAAKTYPE]
 * - The group's [zacClientRoles] contains the specified domain
 *
 * @param domain The domain to check access for
 * @return true if the group has access to the domain, false otherwise
 */
fun Group.hasAccessTo(domain: String?) =
    (domain == null || domain == ROL_DOMEIN_ELK_ZAAKTYPE) || this.zacClientRoles.contains(domain)
