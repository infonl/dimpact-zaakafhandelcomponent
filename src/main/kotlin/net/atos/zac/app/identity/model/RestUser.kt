/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.identity.model

import nl.info.zac.identity.model.User
import nl.info.zac.identity.model.getFullName
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RestUser(
    var id: String,

    var naam: String
)

fun User.toRestUser() =
    RestUser(
        this.id,
        this.getFullName()
    )

fun List<User>.toRestUsers(): List<RestUser> =
    this.map { it.toRestUser() }
