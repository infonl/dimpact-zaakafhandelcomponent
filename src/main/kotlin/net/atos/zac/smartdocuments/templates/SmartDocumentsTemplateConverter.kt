package net.atos.zac.smartdocuments.templates

import net.atos.client.smartdocuments.model.templates.SmartDocumentsResponseTemplate
import net.atos.client.smartdocuments.model.templates.SmartDocumentsResponseTemplateGroup
import net.atos.client.smartdocuments.model.templates.SmartDocumentsTemplatesResponse
import net.atos.zac.smartdocuments.rest.RESTSmartDocumentsTemplate
import net.atos.zac.smartdocuments.rest.RESTSmartDocumentsTemplateGroup
import net.atos.zac.smartdocuments.templates.model.SmartDocumentsTemplate
import net.atos.zac.smartdocuments.templates.model.SmartDocumentsTemplateGroup
import java.time.ZonedDateTime

@Suppress("TooManyFunctions")
object SmartDocumentsTemplateConverter {

    fun SmartDocumentsTemplatesResponse.toREST(): Set<RESTSmartDocumentsTemplateGroup> =
        this.documentsStructure.templatesStructure.templateGroups
            .mapTo(mutableSetOf()) { convertTemplateGroupToREST(it) }

    private fun convertTemplateGroupToREST(
        group: SmartDocumentsResponseTemplateGroup
    ): RESTSmartDocumentsTemplateGroup {
        val restGroup = createRESTTemplateGroup(group)

        if (!group.templates.isNullOrEmpty()) {
            restGroup.templates = mutableSetOf<RESTSmartDocumentsTemplate>().apply {
                group.templates?.forEach {
                    add(createRESTTemplate(it))
                }
            }
        }

        if (!group.templateGroups.isNullOrEmpty()) {
            restGroup.groups = mutableSetOf<RESTSmartDocumentsTemplateGroup>().apply {
                group.templateGroups?.forEach {
                    add(convertTemplateGroupToREST(it))
                }
            }
        }

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

    fun Set<RESTSmartDocumentsTemplateGroup>.toModel(): Set<SmartDocumentsTemplateGroup> =
        this.mapTo(mutableSetOf()) { convertTemplateGroupToModel(it, null) }

    private fun convertTemplateGroupToModel(
        group: RESTSmartDocumentsTemplateGroup,
        parent: SmartDocumentsTemplateGroup?
    ): SmartDocumentsTemplateGroup {
        val jpaGroup = createModelTemplateGroup(group, parent)

        group.templates?.forEach {
            jpaGroup.templates.add(createModelTemplate(it))
        }

        group.groups?.forEach {
            jpaGroup.children.add(convertTemplateGroupToModel(it, jpaGroup))
        }

        return jpaGroup
    }

    private fun createModelTemplateGroup(
        smartDocumentsTemplateGroup: RESTSmartDocumentsTemplateGroup,
        parentGroup: SmartDocumentsTemplateGroup?
    ) = SmartDocumentsTemplateGroup().apply {
        smartDocumentsId = smartDocumentsTemplateGroup.id
        name = smartDocumentsTemplateGroup.name
        parent = parentGroup
        creationDate = ZonedDateTime.now()
    }

    private fun createModelTemplate(
        smartDocumentsTemplate: RESTSmartDocumentsTemplate
    ) = SmartDocumentsTemplate().apply {
        smartDocumentsId = smartDocumentsTemplate.id
        name = smartDocumentsTemplate.name
        creationDate = ZonedDateTime.now()
    }

    fun SmartDocumentsTemplatesResponse.toModel(): Set<SmartDocumentsTemplateGroup> =
        this.documentsStructure.templatesStructure.templateGroups
            .mapTo(mutableSetOf()) { convertTemplateGroupToModel(it, null) }

    private fun convertTemplateGroupToModel(
        group: SmartDocumentsResponseTemplateGroup,
        parent: SmartDocumentsTemplateGroup?
    ): SmartDocumentsTemplateGroup {
        val jpaGroup = createModelTemplateGroup(group, parent)

        group.templates?.forEach {
            jpaGroup.templates.add(createModelTemplate(it))
        }

        group.templateGroups?.forEach {
            jpaGroup.children.add(convertTemplateGroupToModel(it, jpaGroup))
        }

        return jpaGroup
    }

    private fun createModelTemplateGroup(
        smartDocumentsTemplateGroup: SmartDocumentsResponseTemplateGroup,
        parentGroup: SmartDocumentsTemplateGroup?
    ) = SmartDocumentsTemplateGroup().apply {
        smartDocumentsId = smartDocumentsTemplateGroup.id
        name = smartDocumentsTemplateGroup.name
        parent = parentGroup
        creationDate = ZonedDateTime.now()
    }

    private fun createModelTemplate(
        smartDocumentsTemplate: SmartDocumentsResponseTemplate
    ) = SmartDocumentsTemplate().apply {
        smartDocumentsId = smartDocumentsTemplate.id
        name = smartDocumentsTemplate.name
        creationDate = ZonedDateTime.now()
    }
}
