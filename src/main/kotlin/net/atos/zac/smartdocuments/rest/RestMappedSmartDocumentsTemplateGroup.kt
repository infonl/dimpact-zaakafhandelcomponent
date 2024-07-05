/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.smartdocuments.rest

import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RestMappedSmartDocumentsTemplateGroup(
    var id: String,
    var name: String,
    var groups: Set<RestMappedSmartDocumentsTemplateGroup>?,
    var templates: Set<RestMappedSmartDocumentsTemplate>?,
)

fun Set<RestMappedSmartDocumentsTemplateGroup>.toStringRepresentation(): Set<String> =
    this.flatMap { convertTemplateGroupToStringRepresentation(it, null) }.toSet()

private fun convertTemplateGroupToStringRepresentation(
    group: RestMappedSmartDocumentsTemplateGroup,
    parent: String?
): Set<String> =
    arrayOf(parent, "group.${group.id}.${group.name}").filterNotNull().joinToString(".").let { groupString ->
        mutableSetOf(groupString).apply {
            group.templates?.mapTo(this) { "$groupString.template.${it.id}.${it.name}" }
            group.groups?.map { addAll(convertTemplateGroupToStringRepresentation(it, groupString)) }
        }
    }
