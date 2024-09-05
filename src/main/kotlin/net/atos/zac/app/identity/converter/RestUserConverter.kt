/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.identity.converter

import jakarta.inject.Inject
import net.atos.zac.app.identity.model.RestUser
import net.atos.zac.app.identity.model.toRestUser
import net.atos.zac.identity.IdentityService
import nl.lifely.zac.util.NoArgConstructor

@NoArgConstructor
class RestUserConverter @Inject constructor(
    private val identityService: IdentityService
) {
    fun convertUserId(userId: String) = identityService.readUser(userId).toRestUser()

    fun List<String>.convertUserIds(): List<RestUser> = this.map(::convertUserId)
}
