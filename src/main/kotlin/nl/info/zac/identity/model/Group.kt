/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.identity.model

import org.keycloak.representations.idm.GroupRepresentation

/**
 * Data class representing a group in the identity system.
 *
 * @param name The unique name of the group. The group name is treated as the unique group identification.
 * Therefore, we do not allow group names to be changed.
 * @param description The description of the group.
 * @param email The email address associated with the group, if any.
 * @param zacClientRoles The list of ZAC client roles assigned to the group.
 */
data class Group(
    val name: String,

    val description: String,

    val email: String? = null,

    // TODO: should be realm roles in the new IAM situation.
    // but do we still need this mapping at all then? that's handled by the PABC now?
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
 * Somewhat confusingly, we map the Keycloak group name to our group id,
 * and the Keycloak group description field (if any) to our group name.
 * The Keycloak group id is a UUID and is not of interest to us.
 * The Keycloak group name is the unique group name in the ZAC Keycloak realm,
 * and we treat this as the group id in our system.
 *
 * @param keycloakClientId The client ID of the Keycloak client.
 * @return A [Group] object representing the group.
 */
fun GroupRepresentation.toGroup(keycloakClientId: String): Group =
    Group(
        name = name,
        description = description?.takeIf { it.isNotBlank() } ?: name,
        email = attributes?.get("email")?.singleOrNull(),
        // TODO: should be realm roles in the new IAM situation.
        // but do we still need this mapping at all then? that's handled by the PABC now?
        zacClientRoles = clientRoles[keycloakClientId].orEmpty()
    )

fun nl.info.client.pabc.model.generated.GroupRepresentation.toGroup(): Group =
    Group(
        name = name,
        description = description
    )
