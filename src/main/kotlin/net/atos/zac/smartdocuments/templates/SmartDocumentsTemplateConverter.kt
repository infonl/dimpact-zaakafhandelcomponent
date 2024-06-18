/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.smartdocuments.templates

import net.atos.client.smartdocuments.model.templates.SmartDocumentsResponseTemplate
import net.atos.client.smartdocuments.model.templates.SmartDocumentsResponseTemplateGroup
import net.atos.client.smartdocuments.model.templates.SmartDocumentsTemplatesResponse
import net.atos.zac.smartdocuments.rest.RESTSmartDocumentsTemplate
import net.atos.zac.smartdocuments.rest.RESTSmartDocumentsTemplateGroup
import net.atos.zac.smartdocuments.templates.model.SmartDocumentsTemplate
import net.atos.zac.smartdocuments.templates.model.SmartDocumentsTemplateGroup
import net.atos.zac.zaaksturing.model.ZaakafhandelParameters
import java.time.ZonedDateTime

@Suppress("TooManyFunctions")
object SmartDocumentsTemplateConverter {

    fun SmartDocumentsTemplatesResponse.toREST(): Set<RESTSmartDocumentsTemplateGroup> =
        this.documentsStructure.templatesStructure.templateGroups
            .mapTo(mutableSetOf()) { convertTemplateGroupResponseToREST(it) }

    private fun convertTemplateGroupResponseToREST(
        group: SmartDocumentsResponseTemplateGroup
    ): RESTSmartDocumentsTemplateGroup {
        val restGroup = createRESTTemplateGroup(group)

        restGroup.templates = group.templates?.map {
            createRESTTemplate(it)
        }?.ifEmpty { null }?.toSet()
        restGroup.groups = group.templateGroups?.map {
            convertTemplateGroupResponseToREST(it)
        }?.ifEmpty { null }?.toSet()

        return restGroup
    }

    private fun createRESTTemplateGroup(
        smartDocumentsTemplateGroup: SmartDocumentsResponseTemplateGroup
    ) = RESTSmartDocumentsTemplateGroup(
        id = smartDocumentsTemplateGroup.id,
        name = smartDocumentsTemplateGroup.name,
        groups = null,
        templates = null
    )

    private fun createRESTTemplate(
        smartDocumentsTemplate: SmartDocumentsResponseTemplate
    ) = RESTSmartDocumentsTemplate(
        id = smartDocumentsTemplate.id,
        name = smartDocumentsTemplate.name
    )

    fun Set<RESTSmartDocumentsTemplateGroup>.toModel(
        zaakafhandelParameters: ZaakafhandelParameters
    ): Set<SmartDocumentsTemplateGroup> =
        this.mapTo(mutableSetOf()) { convertTemplateGroupToModel(it, null, zaakafhandelParameters) }

    private fun convertTemplateGroupToModel(
        group: RESTSmartDocumentsTemplateGroup,
        parent: SmartDocumentsTemplateGroup?,
        zaakafhandelParameterId: ZaakafhandelParameters
    ): SmartDocumentsTemplateGroup {
        val jpaGroup = createModelTemplateGroup(group, parent, zaakafhandelParameterId)

        jpaGroup.templates = group.templates?.map {
            createModelTemplate(it, jpaGroup, zaakafhandelParameterId)
        }?.ifEmpty { null }?.toMutableSet()
        jpaGroup.children = group.groups?.map {
            convertTemplateGroupToModel(it, jpaGroup, zaakafhandelParameterId)
        }?.ifEmpty { null }?.toMutableSet()

        return jpaGroup
    }

    private fun createModelTemplateGroup(
        smartDocumentsTemplateGroup: RESTSmartDocumentsTemplateGroup,
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
        smartDocumentsTemplate: RESTSmartDocumentsTemplate,
        parentGroup: SmartDocumentsTemplateGroup,
        zaakafhandelParams: ZaakafhandelParameters
    ) = SmartDocumentsTemplate().apply {
        smartDocumentsId = smartDocumentsTemplate.id
        zaakafhandelParameters = zaakafhandelParams
        name = smartDocumentsTemplate.name
        templateGroup = parentGroup
        creationDate = ZonedDateTime.now()
    }

    fun Set<SmartDocumentsTemplateGroup>.toREST(): Set<RESTSmartDocumentsTemplateGroup> =
        this.mapTo(mutableSetOf()) { convertTemplateGroupToREST(it) }

    private fun convertTemplateGroupToREST(
        group: SmartDocumentsTemplateGroup
    ): RESTSmartDocumentsTemplateGroup {
        val restTemplateGroup = createRESTTemplateGroup(group)

        restTemplateGroup.templates = group.templates?.map {
            createRESTTemplate(it)
        }?.ifEmpty { null }?.toSet()
        restTemplateGroup.groups = group.children?.map {
            convertTemplateGroupToREST(it)
        }?.ifEmpty { null }?.toSet()

        return restTemplateGroup
    }

    private fun createRESTTemplateGroup(
        smartDocumentsTemplateGroup: SmartDocumentsTemplateGroup,
    ) = RESTSmartDocumentsTemplateGroup(
        id = smartDocumentsTemplateGroup.smartDocumentsId,
        name = smartDocumentsTemplateGroup.name,
        groups = null,
        templates = null
    )

    private fun createRESTTemplate(
        smartDocumentsTemplate: SmartDocumentsTemplate
    ) = RESTSmartDocumentsTemplate(
        id = smartDocumentsTemplate.smartDocumentsId,
        name = smartDocumentsTemplate.name
    )

    fun Set<RESTSmartDocumentsTemplateGroup>.toStringRepresentation(): Set<String> {
        val result = mutableSetOf<String>()
        this.map { result.addAll(convertTemplateGroupToStringRepresentation(it, null)) }
        return result
    }

    private fun convertTemplateGroupToStringRepresentation(
        group: RESTSmartDocumentsTemplateGroup,
        parent: String?
    ): Set<String> =
        arrayOf(parent, "group.${group.id}.${group.name}").filterNotNull().joinToString(".").let { groupString ->
            mutableSetOf(groupString).apply {
                group.templates?.mapTo(this) { "$groupString.template.${it.id}.${it.name}" }
                group.groups?.map { addAll(convertTemplateGroupToStringRepresentation(it, groupString)) }
            }
        }
}
