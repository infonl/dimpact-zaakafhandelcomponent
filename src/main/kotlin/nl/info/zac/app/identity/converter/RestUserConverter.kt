/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.identity.converter

import jakarta.inject.Inject
import nl.info.zac.app.identity.model.RestUser
import nl.info.zac.app.identity.model.toRestUser
import nl.info.zac.identity.IdentityService
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
class RestUserConverter @Inject constructor(
    private val identityService: IdentityService
) {
    fun convertUserId(userId: String, logContext: String? = null) =
        identityService.readUser(username = userId, logContext = logContext).toRestUser()

    fun List<String>.convertUserIds(logContext: String? = null): List<RestUser> =
        this.map { convertUserId(userId = it, logContext = logContext) }
}
