/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.sd.model.templates

import nl.lifely.zac.util.NoArgConstructor

@NoArgConstructor
data class TemplateGroup(
    val id: String,
    var name: String,
    var templateGroups: List<TemplateGroup>,
    var templates: List<Template>,
    var accessible: Boolean = false,
    var allDescendants: Boolean = false
)
