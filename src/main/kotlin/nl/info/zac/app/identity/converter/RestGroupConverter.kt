/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.identity.converter

import jakarta.inject.Inject
import nl.info.zac.app.identity.model.toRestGroup
import nl.info.zac.identity.IdentityService
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
class RestGroupConverter @Inject constructor(
    private val identityService: IdentityService
) {
    fun convertGroupId(groupId: String) = identityService.readGroup(groupId).toRestGroup()
}
