/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.sd.model.templates

import nl.lifely.zac.util.NoArgConstructor

@NoArgConstructor
class TemplateGroup {
    lateinit var id: String
    lateinit var name: String
    lateinit var templateGroups: List<TemplateGroup>
    lateinit var templates: List<Template>
    var accessible: Boolean = false
    var allDescendants: Boolean = false
}
