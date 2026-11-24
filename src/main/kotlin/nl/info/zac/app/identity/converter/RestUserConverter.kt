/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.identity.converter

import jakarta.inject.Inject
import nl.info.zac.app.identity.model.RestUser
import nl.info.zac.app.identity.model.toRestUser
import nl.info.zac.identity.IdentityLookupContext
import nl.info.zac.identity.IdentityService
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
class RestUserConverter @Inject constructor(
    private val identityService: IdentityService
) {
    fun convertUsername(username: String, context: IdentityLookupContext? = null) =
        identityService.readUser(username = username, context = context).toRestUser()

    fun List<String>.convertUsernames(context: IdentityLookupContext? = null): List<RestUser> =
        this.map { convertUsername(username = it, context = context) }
}
