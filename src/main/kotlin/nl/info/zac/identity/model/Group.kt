/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.identity.model

import org.keycloak.representations.idm.GroupRepresentation


data class Group(
    val id: String,

    val name: String,

    val email: String? = null,

    val zacClientRoles: List<String> = emptyList()
) {
    /**
     * Constructor for creating an unknown Group, a group with a given group id which is not known in the identity system.
     *
     * @param id ID of the group which is unknown
     */
    constructor(id: String) : this(
        id = id,
        name = id
    )
}

fun GroupRepresentation.toGroup(keycloakClientId: String): Group =
    // Confusingly the terms `group name` and `group description` are used in the Keycloak admin UI,
    // however in the Keycloak API these are called `id` and `name` respectively.
    Group(
        id = name,
        name = attributes?.get("description")?.singleOrNull() ?: name,
        email = attributes?.get("email")?.singleOrNull(),
        // also map the client roles for the ZAC Keycloak client
        zacClientRoles = clientRoles[keycloakClientId].orEmpty()
    )
