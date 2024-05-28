/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.sd.model.templates

import nl.lifely.zac.util.NoArgConstructor

@NoArgConstructor
data class UserGroup(
    val id: String,
    val name: String,
    val groupsAccess: GroupsAccess,
    val userGroups: List<UserGroup>,
    val users: List<User>,
    val accessible: Boolean,
)
