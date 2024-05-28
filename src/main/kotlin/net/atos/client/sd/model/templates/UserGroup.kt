/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.sd.model.templates

import nl.lifely.zac.util.NoArgConstructor

@NoArgConstructor
class UserGroup {
    lateinit var id: String
    lateinit var name: String
    lateinit var groupsAccess: GroupsAccess
    lateinit var userGroups: List<UserGroup>
    lateinit var users: List<User>
    var accessible: Boolean = false
}
