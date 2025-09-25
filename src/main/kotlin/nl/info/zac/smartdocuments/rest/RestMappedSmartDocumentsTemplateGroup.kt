/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.smartdocuments.rest

import net.atos.zac.admin.model.ZaaktypeCmmnConfiguration
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
    var groups: Set<RestMappedSmartDocumentsTemplateGroup>?,
    var templates: Set<RestMappedSmartDocumentsTemplate>?,
)

fun Set<RestMappedSmartDocumentsTemplateGroup>.toStringRepresentation(): Set<String> =
    this.flatMap { convertTemplateGroupToStringRepresentation(it, null) }.toSet()

fun Set<RestMappedSmartDocumentsTemplateGroup>.toSmartDocumentsTemplateGroupSet(
    zaaktypeCmmnConfiguration: ZaaktypeCmmnConfiguration
): Set<SmartDocumentsTemplateGroup> =
    this.mapTo(mutableSetOf()) {
        convertTemplateGroupToModel(it, null, zaaktypeCmmnConfiguration)
    }

fun Set<SmartDocumentsTemplateGroup>.toRestSmartDocumentsTemplateGroup(): Set<RestMappedSmartDocumentsTemplateGroup> =
    this.mapTo(mutableSetOf()) { convertTemplateGroupToRest(it) }

private fun createModelTemplateGroup(
    smartDocumentsTemplateGroup: RestMappedSmartDocumentsTemplateGroup,
    parentGroup: SmartDocumentsTemplateGroup?,
    zaakafhandelParams: ZaaktypeCmmnConfiguration
) = SmartDocumentsTemplateGroup().apply {
    smartDocumentsId = smartDocumentsTemplateGroup.id
    zaaktypeCmmnConfiguration = zaakafhandelParams
    name = smartDocumentsTemplateGroup.name
    parent = parentGroup
    creationDate = ZonedDateTime.now()
}

private fun createModelTemplate(
    smartDocumentsTemplate: RestMappedSmartDocumentsTemplate,
    parentGroup: SmartDocumentsTemplateGroup,
    zaakafhandelParams: ZaaktypeCmmnConfiguration
) = SmartDocumentsTemplate().apply {
    smartDocumentsId = smartDocumentsTemplate.id
    zaaktypeCmmnConfiguration = zaakafhandelParams
    informatieObjectTypeUUID = smartDocumentsTemplate.informatieObjectTypeUUID
    name = smartDocumentsTemplate.name
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
            group.groups?.map { addAll(convertTemplateGroupToStringRepresentation(it, groupString)) }
        }
    }

private fun convertTemplateGroupToRest(
    group: SmartDocumentsTemplateGroup
): RestMappedSmartDocumentsTemplateGroup =
    RestMappedSmartDocumentsTemplateGroup(
        id = group.smartDocumentsId,
        name = group.name,
        groups = group.children?.map { convertTemplateGroupToRest(it) }?.toSet(),
        templates = group.templates?.map {
            RestMappedSmartDocumentsTemplate(it.smartDocumentsId, it.name, it.informatieObjectTypeUUID)
        }?.toSet()
    )

private fun convertTemplateGroupToModel(
    group: RestMappedSmartDocumentsTemplateGroup,
    parent: SmartDocumentsTemplateGroup?,
    zaakafhandelParameterId: ZaaktypeCmmnConfiguration
): SmartDocumentsTemplateGroup =
    createModelTemplateGroup(group, parent, zaakafhandelParameterId).apply {
        templates = group.templates?.map {
            createModelTemplate(it, this, zaakafhandelParameterId)
        }?.toMutableSet()
        children = group.groups?.map {
            convertTemplateGroupToModel(it, this, zaakafhandelParameterId)
        }?.toMutableSet()
    }
