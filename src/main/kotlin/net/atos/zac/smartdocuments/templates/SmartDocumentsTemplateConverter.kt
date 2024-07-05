/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.smartdocuments.templates

import net.atos.client.smartdocuments.model.templates.SmartDocumentsResponseTemplateGroup
import net.atos.client.smartdocuments.model.templates.SmartDocumentsTemplatesResponse
import net.atos.zac.smartdocuments.rest.RestMappedSmartDocumentsTemplate
import net.atos.zac.smartdocuments.rest.RestMappedSmartDocumentsTemplateGroup
import net.atos.zac.smartdocuments.rest.RestSmartDocumentsTemplate
import net.atos.zac.smartdocuments.rest.RestSmartDocumentsTemplateGroup
import net.atos.zac.smartdocuments.templates.model.SmartDocumentsTemplate
import net.atos.zac.smartdocuments.templates.model.SmartDocumentsTemplateGroup
import net.atos.zac.zaaksturing.model.ZaakafhandelParameters
import java.time.ZonedDateTime

@Suppress("TooManyFunctions")
object SmartDocumentsTemplateConverter {

    /**
     * SmartDocuments --> REST
     */
    fun SmartDocumentsTemplatesResponse.toREST(): Set<RestSmartDocumentsTemplateGroup> =
        this.documentsStructure.templatesStructure.templateGroups
            .mapTo(mutableSetOf()) { convertTemplateGroupResponseToREST(it) }

    private fun convertTemplateGroupResponseToREST(
        group: SmartDocumentsResponseTemplateGroup
    ): RestSmartDocumentsTemplateGroup =
        RestSmartDocumentsTemplateGroup(
            id = group.id,
            name = group.name,
            groups = group.templateGroups?.map { convertTemplateGroupResponseToREST(it) }?.ifEmpty { null }?.toSet(),
            templates = group.templates?.map { RestSmartDocumentsTemplate(it.id, it.name) }?.ifEmpty { null }?.toSet()
        )

    /**
     * REST --> JPA
     */
    fun Set<RestMappedSmartDocumentsTemplateGroup>.toModel(
        zaakafhandelParameters: ZaakafhandelParameters
    ): Set<SmartDocumentsTemplateGroup> =
        this.mapTo(mutableSetOf()) {
            convertTemplateGroupToModel(it, null, zaakafhandelParameters)
        }

    private fun convertTemplateGroupToModel(
        group: RestMappedSmartDocumentsTemplateGroup,
        parent: SmartDocumentsTemplateGroup?,
        zaakafhandelParameterId: ZaakafhandelParameters
    ): SmartDocumentsTemplateGroup {
        val jpaGroup = createModelTemplateGroup(group, parent, zaakafhandelParameterId)

        jpaGroup.templates = group.templates?.map {
            createModelTemplate(it as RestMappedSmartDocumentsTemplate, jpaGroup, zaakafhandelParameterId)
        }?.ifEmpty { null }?.toMutableSet()
        jpaGroup.children = group.groups?.map {
            convertTemplateGroupToModel(it as RestMappedSmartDocumentsTemplateGroup, jpaGroup, zaakafhandelParameterId)
        }?.ifEmpty { null }?.toMutableSet()

        return jpaGroup
    }

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

    /**
     * JPA --> REST
     */
    fun Set<SmartDocumentsTemplateGroup>.toREST(): Set<RestMappedSmartDocumentsTemplateGroup> =
        this.mapTo(mutableSetOf()) { convertTemplateGroupToREST(it) }

    private fun convertTemplateGroupToREST(
        group: SmartDocumentsTemplateGroup
    ): RestMappedSmartDocumentsTemplateGroup =
        RestMappedSmartDocumentsTemplateGroup(
            id = group.smartDocumentsId,
            name = group.name,
            groups = group.children?.map { convertTemplateGroupToREST(it) }?.ifEmpty { null }?.toSet(),
            templates = group.templates?.map {
                RestMappedSmartDocumentsTemplate(it.smartDocumentsId, it.name, it.informatieObjectTypeUUID)
            }?.ifEmpty { null }?.toSet()
        )
}
