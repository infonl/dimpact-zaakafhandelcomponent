/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.identity.model

import org.keycloak.representations.idm.UserRepresentation

open class User(
    val id: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val displayName: String? = null,
    val email: String? = null
) {
    /**
     * Constructor for creating an unknown User, a user with a given user id which is not known in the identity system.
     *
     * @param id ID of the user who is unknown
     */
    constructor(id: String) : this(
        id = id,
        firstName = null,
        lastName = id,
        email = null
    )
}

/**
 * Returns the full name of the user using the following logic:
 * - If the display name is present, return that
 * - Else if both first name and last name are present, concatenate them with a space and return that
 * - Else if only the last name is present, return the last name
 * - Else if the user does not have a last name, return the user id
 *
 * Note that we do not support an infix (tussenvoegsel) in the name
 */
fun User.getFullName(): String =
    when {
        !displayName.isNullOrBlank() -> displayName
        !firstName.isNullOrBlank() && !lastName.isNullOrBlank() -> "$firstName $lastName"
        !lastName.isNullOrBlank() -> lastName
        else -> id
    }

fun UserRepresentation.toUser(): User =
    User(
        // we use the username as the user id and not the internal Keycloak user id
        id = username,
        firstName = firstName,
        lastName = lastName,
        email = email
    )
