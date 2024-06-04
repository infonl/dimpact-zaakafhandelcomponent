package net.atos.zac.smartdocuments.templates

import net.atos.client.smartdocuments.model.templates.SmartDocumentsResponseTemplate
import net.atos.client.smartdocuments.model.templates.SmartDocumentsResponseTemplateGroup
import net.atos.client.smartdocuments.model.templates.SmartDocumentsTemplatesResponse
import net.atos.zac.smartdocuments.templates.model.SmartDocumentsTemplate
import net.atos.zac.smartdocuments.templates.model.SmartDocumentsTemplateGroup
import java.time.ZonedDateTime

@Suppress("TooManyFunctions")
object SmartDocumentsTemplateConverter {

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
