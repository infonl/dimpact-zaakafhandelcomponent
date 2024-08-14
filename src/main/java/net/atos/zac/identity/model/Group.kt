/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.identity.model

data class Group(
    val id: String,

    // TODO: non-nullable?
    val name: String? = null,

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
//
//    constructor(id: String, name: String?, email: String?) {
//        this.id = id
//        this.name = name ?: id
//        this.email = email
//    }
// }
