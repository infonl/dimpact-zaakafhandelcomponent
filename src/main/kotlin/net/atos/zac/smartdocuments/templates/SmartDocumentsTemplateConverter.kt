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
                    add(convertTemplateGroupResponseToREST(it))
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

        group.templates?.forEach {
            jpaGroup.templates.add(createModelTemplate(it, jpaGroup, zaakafhandelParameterId))
        }

        group.groups?.forEach {
            jpaGroup.children.add(convertTemplateGroupToModel(it, jpaGroup, zaakafhandelParameterId))
        }

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

        if (group.templates.isNotEmpty()) {
            restTemplateGroup.templates = mutableSetOf<RESTSmartDocumentsTemplate>().apply {
                group.templates.forEach {
                    add(createRESTTemplate(it))
                }
            }
        }

        if (group.children.isNotEmpty()) {
            restTemplateGroup.groups = mutableSetOf<RESTSmartDocumentsTemplateGroup>().apply {
                group.children.forEach {
                    add(convertTemplateGroupToREST(it))
                }
            }
        }

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
}
