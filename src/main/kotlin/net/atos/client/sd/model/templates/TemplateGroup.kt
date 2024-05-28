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
    var allDescendants: Boolean = false

    var templateGroups: List<TemplateGroup>? = null
    var templates: List<Template>? = null
    var accessible: Boolean? = null
}
