/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.identity.model

import org.keycloak.representations.idm.GroupRepresentation

data class Group(
    val id: String,

    val name: String,

    val email: String? = null
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

fun GroupRepresentation.toGroup(): Group =
    Group(
        // better to rename 'id' field as 'name' and 'name' field as 'description'
        id = name,
        // maybe we want a separate description field in the Group class
        name = attributes?.get("description")?.single()?.toString() ?: name,
        email = attributes?.get("email")?.single().toString()
    )
