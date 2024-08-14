/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.identity.converter

import jakarta.inject.Inject
import net.atos.zac.app.identity.model.RestGroup
import net.atos.zac.identity.IdentityService
import net.atos.zac.identity.model.Group
import nl.lifely.zac.util.NoArgConstructor

@NoArgConstructor
class RestGroupConverter @Inject constructor(
    private val identityService: IdentityService
) {
    fun convertGroups(groups: List<Group>): List<RestGroup> {
        return groups.map { this.convertGroup(it) }
    }

    private fun convertGroup(group: Group): RestGroup =
        RestGroup(group.id, group.name)

    fun convertGroupId(groupId: String): RestGroup =
        convertGroup(identityService.readGroup(groupId))
}
