/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.sd.model.templates

import nl.lifely.zac.util.NoArgConstructor

@NoArgConstructor
data class GroupsAccess(
    var templateGroups: List<TemplateGroup>,
    // TODO: what do these contain?
    var headerGroups: List<Any>
)
