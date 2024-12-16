/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.identity.model

import org.keycloak.representations.idm.UserRepresentation

open class User(
    val id: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val fullName: String? = null,
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
        fullName = id,
        email = null
    )
}

/**
 * Maybe better to get rid of this extension function by using the `fullName` property directly.
 * Then we need to make sure the full name gets set correctly in the first place.
 */
fun User.getFullName(): String =
    when {
        !fullName.isNullOrBlank() -> fullName
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
        // note that we do not support infixes (tussenvoegsels) (yet)
        fullName = "$firstName $lastName",
        email = email
    )
