/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.smartdocuments.rest

import net.atos.zac.admin.model.ZaakafhandelParameters
import net.atos.zac.smartdocuments.templates.model.SmartDocumentsTemplate
import net.atos.zac.smartdocuments.templates.model.SmartDocumentsTemplateGroup
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
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
    zaakafhandelParameters: ZaakafhandelParameters
): Set<SmartDocumentsTemplateGroup> =
    this.mapTo(mutableSetOf()) {
        convertTemplateGroupToModel(it, null, zaakafhandelParameters)
    }

fun Set<SmartDocumentsTemplateGroup>.toRestSmartDocumentsTemplateGroup(): Set<RestMappedSmartDocumentsTemplateGroup> =
    this.mapTo(mutableSetOf()) { convertTemplateGroupToRest(it) }

private fun createModelTemplateGroup(
    smartDocumentsTemplateGroup: RestMappedSmartDocumentsTemplateGroup,
    parentGroup: SmartDocumentsTemplateGroup?,
    zaakafhandelParams: ZaakafhandelParameters
) = SmartDocumentsTemplateGroup().apply {
    smartDocumentsId = smartDocumentsTemplateGroup.id
    zaakafhandelParameters = zaakafhandelParams
    name = smartDocumentsTemplateGroup.name
    parent = parentGroup
    creationDate = ZonedDateTime.now()
}

private fun createModelTemplate(
    smartDocumentsTemplate: RestMappedSmartDocumentsTemplate,
    parentGroup: SmartDocumentsTemplateGroup,
    zaakafhandelParams: ZaakafhandelParameters
) = SmartDocumentsTemplate().apply {
    smartDocumentsId = smartDocumentsTemplate.id
    zaakafhandelParameters = zaakafhandelParams
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
    zaakafhandelParameterId: ZaakafhandelParameters
): SmartDocumentsTemplateGroup =
    createModelTemplateGroup(group, parent, zaakafhandelParameterId).apply {
        templates = group.templates?.map {
            createModelTemplate(it, this, zaakafhandelParameterId)
        }?.toMutableSet()
        children = group.groups?.map {
            convertTemplateGroupToModel(it, this, zaakafhandelParameterId)
        }?.toMutableSet()
    }
