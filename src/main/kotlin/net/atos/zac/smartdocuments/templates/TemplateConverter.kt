package net.atos.zac.smartdocuments.templates

import net.atos.client.smartdocuments.model.templates.SmartDocumentsResponseTemplate
import net.atos.client.smartdocuments.model.templates.SmartDocumentsResponseTemplateGroup
import net.atos.client.smartdocuments.model.templates.SmartDocumentsTemplatesResponse
import net.atos.zac.smartdocuments.templates.model.SmartDocumentsTemplate
import net.atos.zac.smartdocuments.templates.model.SmartDocumentsTemplateGroup
import java.time.ZonedDateTime

object TemplateConverter {
    fun convert(response: SmartDocumentsTemplatesResponse): Set<SmartDocumentsTemplateGroup> =
        response.documentsStructure.templatesStructure.templateGroups
            .mapTo(mutableSetOf()) { convertTemplateGroup(it, null) }

    private fun convertTemplateGroup(group: SmartDocumentsResponseTemplateGroup, parent: SmartDocumentsTemplateGroup?): SmartDocumentsTemplateGroup {
        val jpaGroup = createTemplateGroup(group, parent)

        group.templates?.forEach {
            jpaGroup.templates.add(createTemplate(it))
        }

        group.templateGroups?.forEach {
            jpaGroup.children.add(convertTemplateGroup(it, jpaGroup))
        }

        return jpaGroup
    }

    private fun createTemplateGroup(
        smartDocumentsTemplateGroup: SmartDocumentsResponseTemplateGroup,
        parentGroup: SmartDocumentsTemplateGroup?
    ) = SmartDocumentsTemplateGroup().apply {
        smartDocumentsId = smartDocumentsTemplateGroup.id
        name = smartDocumentsTemplateGroup.name
        parent = parentGroup
        creationDate = ZonedDateTime.now()
    }

    private fun createTemplate(smartDocumentsTemplate: SmartDocumentsResponseTemplate) = SmartDocumentsTemplate().apply {
        smartDocumentsId = smartDocumentsTemplate.id
        name = smartDocumentsTemplate.name
        creationDate = ZonedDateTime.now()
    }
}
