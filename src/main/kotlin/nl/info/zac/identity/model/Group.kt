/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.identity.model

import org.keycloak.representations.idm.GroupRepresentation

/**
 * Data class representing a group in the identity system.
 */
data class Group(
    /**
     * The unique name of the group. The group name is treated as the unique group identification.
     *  Therefore, we do not allow group names to be changed.
     */
    val name: String,

    /**
     * The description of the group. If no description exists in the identity system, the name is used as description.
     */
    val description: String,

    /**
     * The email address associated with the group, if any.
     */
    val email: String? = null,

    /**
     * The list of Keycloak ZAC client roles assigned to the group.
     */
    @Deprecated(
        """
        ZAC client roles are only used in the old IAM architecture (PABC feature flag turned off). 
        Once the PABC feature flag has been removed, this field should be removed.
        """
    )
    val zacClientRoles: List<String> = emptyList()
) {
    /**
     * Constructor for creating a group not known in the identity system.
     *
     * @param id ID of the group which is unknown
     */
    constructor(id: String) : this(
        name = id,
        description = id
    )
}

/**
 * Converts a [GroupRepresentation] to a [Group], mapping the Keycloak
 * group name, group description, and Keycloak ZAC client roles.
 * We do not map the Keycloak group id (a UUID) as it is not of interest to us.
 * We treat the Keycloak group name as the unique identifier of the group within Keycloak realm.
 *
 * @param keycloakClientId The client ID of the Keycloak client.
 * @return A [Group] object representing the group.
 */
fun GroupRepresentation.toGroup(keycloakClientId: String): Group =
    Group(
        name = name,
        description = description?.takeIf { it.isNotBlank() } ?: name,
        email = attributes?.get("email")?.singleOrNull(),
        // ZAC client roles are only used in the old IAM architecture (PABC feature flag off)
        zacClientRoles = clientRoles[keycloakClientId].orEmpty()
    )

fun nl.info.client.pabc.model.generated.GroupRepresentation.toGroup(): Group =
    Group(
        name = name,
        description = description?.takeIf { it.isNotBlank() } ?: name
    )
