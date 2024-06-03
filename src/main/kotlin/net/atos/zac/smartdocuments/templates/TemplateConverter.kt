package net.atos.zac.smartdocuments.templates

import net.atos.client.smartdocuments.model.templates.SmartDocumentsTemplate
import net.atos.client.smartdocuments.model.templates.SmartDocumentsTemplateGroup
import net.atos.client.smartdocuments.model.templates.TemplatesResponse
import net.atos.zac.smartdocuments.templates.model.Template
import net.atos.zac.smartdocuments.templates.model.TemplateGroup
import java.time.ZonedDateTime

object TemplateConverter {
    fun convert(response: TemplatesResponse): Set<TemplateGroup> =
        response.documentsStructure.templatesStructure.templateGroups
            .mapTo(mutableSetOf()) { convertTemplateGroup(it, null) }

    private fun convertTemplateGroup(group: SmartDocumentsTemplateGroup, parent: TemplateGroup?): TemplateGroup {
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
        smartDocumentsTemplateGroup: SmartDocumentsTemplateGroup,
        parentGroup: TemplateGroup?
    ) = TemplateGroup().apply {
        smartDocumentsId = smartDocumentsTemplateGroup.id
        name = smartDocumentsTemplateGroup.name
        parent = parentGroup
        creationDate = ZonedDateTime.now()
    }

    private fun createTemplate(smartDocumentsTemplate: SmartDocumentsTemplate) = Template().apply {
        smartDocumentsId = smartDocumentsTemplate.id
        name = smartDocumentsTemplate.name
        creationDate = ZonedDateTime.now()
    }
}
