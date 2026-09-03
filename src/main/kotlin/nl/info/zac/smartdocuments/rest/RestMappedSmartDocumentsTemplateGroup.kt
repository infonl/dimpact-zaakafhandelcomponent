/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.smartdocuments.rest

import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.smartdocuments.templates.model.SmartDocumentsTemplate
import nl.info.zac.smartdocuments.templates.model.SmartDocumentsTemplateGroup
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.ZonedDateTime

@NoArgConstructor
@AllOpen
data class RestMappedSmartDocumentsTemplateGroup(
    var id: String,
    var name: String,
    var groups: Set<RestMappedSmartDocumentsTemplateGroup>? = null,
    var templates: Set<RestMappedSmartDocumentsTemplate>? = null,
)

fun Set<RestMappedSmartDocumentsTemplateGroup>.toStringRepresentation(): Set<String> =
    this.flatMap { convertTemplateGroupToStringRepresentation(it, null) }.toSet()

fun Set<RestMappedSmartDocumentsTemplateGroup>.toSmartDocumentsTemplateGroupSet(
    zaaktypeConfiguration: ZaaktypeConfiguration
): Set<SmartDocumentsTemplateGroup> =
    this.mapTo(mutableSetOf()) {
        convertTemplateGroupToModel(it, null, zaaktypeConfiguration)
    }

fun Set<SmartDocumentsTemplateGroup>.toRestSmartDocumentsTemplateGroup(): Set<RestMappedSmartDocumentsTemplateGroup> =
    this.mapTo(mutableSetOf()) { convertTemplateGroupToRest(it) }

private fun createModelTemplateGroup(
    smartDocumentsTemplateGroup: RestMappedSmartDocumentsTemplateGroup,
    parentGroup: SmartDocumentsTemplateGroup?,
    zaakafhandelParams: ZaaktypeConfiguration
) = SmartDocumentsTemplateGroup().apply {
    smartDocumentsId = smartDocumentsTemplateGroup.id
    zaaktypeConfiguration = zaakafhandelParams
    parent = parentGroup
    creationDate = ZonedDateTime.now()
}

private fun createModelTemplate(
    smartDocumentsTemplate: RestMappedSmartDocumentsTemplate,
    parentGroup: SmartDocumentsTemplateGroup,
    zaakafhandelParams: ZaaktypeConfiguration
) = SmartDocumentsTemplate().apply {
    smartDocumentsId = smartDocumentsTemplate.id
    zaaktypeConfiguration = zaakafhandelParams
    informatieObjectTypeUUID = smartDocumentsTemplate.informatieObjectTypeUUID
    templateGroup = parentGroup
    creationDate = ZonedDateTime.now()
}

private fun convertTemplateGroupToStringRepresentation(
    group: RestMappedSmartDocumentsTemplateGroup,
    parent: String?
): Set<String> =
    arrayOf(parent, "group.${group.id}.${group.name}").filterNotNull().joinToString(".").let { groupString ->
        mutableSetOf(groupString).apply {
            group.templates?.mapTo(this) { "$groupString.template.${it.id}.${it.name}" }
            group.groups?.forEach { addAll(convertTemplateGroupToStringRepresentation(it, groupString)) }
        }
    }

// The persisted entity no longer carries a name (see resolveCurrentNames below, which is always
// applied to this function's output and replaces this placeholder with the current SmartDocuments name).
private const val PLACEHOLDER_NAME_REPLACED_BY_RESOLVE_CURRENT_NAMES = ""

private fun convertTemplateGroupToRest(
    group: SmartDocumentsTemplateGroup
): RestMappedSmartDocumentsTemplateGroup =
    RestMappedSmartDocumentsTemplateGroup(
        id = group.smartDocumentsId,
        name = PLACEHOLDER_NAME_REPLACED_BY_RESOLVE_CURRENT_NAMES,
        groups = group.children?.map { convertTemplateGroupToRest(it) }?.toSet(),
        templates = group.templates?.map {
            RestMappedSmartDocumentsTemplate(
                id = it.smartDocumentsId,
                name = PLACEHOLDER_NAME_REPLACED_BY_RESOLVE_CURRENT_NAMES,
                informatieObjectTypeUUID = it.informatieObjectTypeUUID
            )
        }?.toSet()
    )

private fun convertTemplateGroupToModel(
    group: RestMappedSmartDocumentsTemplateGroup,
    parent: SmartDocumentsTemplateGroup?,
    zaakafhandelParameterId: ZaaktypeConfiguration
): SmartDocumentsTemplateGroup =
    createModelTemplateGroup(group, parent, zaakafhandelParameterId).apply {
        templates = group.templates?.map {
            createModelTemplate(it, this, zaakafhandelParameterId)
        }?.toMutableSet()
        children = group.groups?.map {
            convertTemplateGroupToModel(it, this, zaakafhandelParameterId)
        }?.toMutableSet()
    }

/**
 * Replaces every persisted `name` in this mapping with the current SmartDocuments name, matched by id,
 * and drops any group or template whose id no longer exists in SmartDocuments. The persisted
 * `informatieObjectTypeUUID` per template is preserved unchanged.
 */
fun Set<RestMappedSmartDocumentsTemplateGroup>.resolveCurrentNames(
    currentTemplateGroups: Set<RestSmartDocumentsTemplateGroup>
): Set<RestMappedSmartDocumentsTemplateGroup> =
    mapNotNull { it.resolveCurrentNames(currentTemplateGroups) }.toSet()

private fun RestMappedSmartDocumentsTemplateGroup.resolveCurrentNames(
    currentTemplateGroups: Set<RestSmartDocumentsTemplateGroup>
): RestMappedSmartDocumentsTemplateGroup? =
    currentTemplateGroups.findGroupById(id)?.let { currentGroup ->
        RestMappedSmartDocumentsTemplateGroup(
            id = id,
            name = currentGroup.name,
            groups = groups?.resolveCurrentNames(currentTemplateGroups),
            templates = templates?.mapNotNull { template ->
                currentTemplateGroups.findTemplateById(template.id)?.let { currentTemplate ->
                    RestMappedSmartDocumentsTemplate(
                        id = template.id,
                        name = currentTemplate.name,
                        informatieObjectTypeUUID = template.informatieObjectTypeUUID
                    )
                }
            }?.toSet()
        )
    }
