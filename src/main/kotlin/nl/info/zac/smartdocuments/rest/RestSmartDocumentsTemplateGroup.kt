/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.smartdocuments.rest

import nl.info.client.smartdocuments.model.template.SmartDocumentsResponseTemplateGroup
import nl.info.client.smartdocuments.model.template.SmartDocumentsTemplatesResponse
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class RestSmartDocumentsTemplateGroup(
    var id: String,
    var name: String,
    var groups: Set<RestSmartDocumentsTemplateGroup>?,
    var templates: Set<RestSmartDocumentsTemplate>?,
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

fun SmartDocumentsTemplatesResponse.toRestSmartDocumentsTemplateGroupSet(): Set<RestSmartDocumentsTemplateGroup> =
    this.documentsStructure.templatesStructure.templateGroups
        .mapTo(mutableSetOf()) { convertTemplateGroupResponseToRest(it) }

private fun convertTemplateGroupResponseToRest(
    group: SmartDocumentsResponseTemplateGroup
): RestSmartDocumentsTemplateGroup =
    RestSmartDocumentsTemplateGroup(
        id = group.id,
        name = group.name,
        groups = group.templateGroups?.map { convertTemplateGroupResponseToRest(it) }?.toSet(),
        templates = group.templates?.map { RestSmartDocumentsTemplate(it.id, it.name) }?.toSet()
    )

fun Set<RestSmartDocumentsTemplateGroup>.group(groupName: String) =
    this.find { it.name == groupName }

fun Set<RestSmartDocumentsTemplateGroup>.group(groupNames: List<String>): RestSmartDocumentsTemplateGroup {
    var groups = this
    groupNames.take(groupNames.size - 1).forEach { groupName ->
        groups = groups.group(groupName)?.groups
            ?: throw IllegalArgumentException("Group '$groupName' from '$groupNames' not found")
    }
    return groupNames.last().let {
        groups.group(it) ?: throw IllegalArgumentException("Group '$it' from '$groupNames' not found")
    }
}
