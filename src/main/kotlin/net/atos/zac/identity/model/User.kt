/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.identity.model

import org.apache.commons.lang3.StringUtils

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

// TODO: what the heck is this.. get rid of this function?
fun User.getFullNameResolved(): String {
    return if (StringUtils.isNotBlank(fullName)) {
        fullName!!
    } else if (StringUtils.isNotBlank(firstName) && StringUtils.isNotBlank(lastName)) {
        "$firstName $lastName"
    } else if (StringUtils.isNotBlank(lastName)) {
        lastName!!
    } else {
        id
    }
}
