/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.smartdocuments.rest

import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RestSmartDocumentsTemplateGroup(
    val id: String,
    val name: String,
    val groups: Set<RestSmartDocumentsTemplateGroup>?,
    val templates: Set<RestSmartDocumentsTemplate>?,
)

fun Set<RestSmartDocumentsTemplateGroup>.toStringRepresentation(): Set<String> {
    val result = mutableSetOf<String>()
    this.forEach { result.addAll(convertTemplateGroupToStringRepresentation(it, null)) }
    return result
}

private fun convertTemplateGroupToStringRepresentation(
    group: RestSmartDocumentsTemplateGroup,
    parent: String?
): Set<String> =
    arrayOf(parent, "group.${group.id}.${group.name}").filterNotNull().joinToString(".").let { groupString ->
        mutableSetOf(groupString).apply {
            group.templates?.mapTo(this) { "$groupString.template.${it.id}.${it.name}" }
            group.groups?.map { addAll(convertTemplateGroupToStringRepresentation(it, groupString)) }
        }
    }
