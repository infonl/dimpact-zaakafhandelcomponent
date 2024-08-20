/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.smartdocuments

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType.REQUIRED
import jakarta.transaction.Transactional.TxType.SUPPORTS
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.admin.model.ZaakafhandelParameters
import net.atos.zac.documentcreation.DocumentCreationService
import net.atos.zac.smartdocuments.rest.RestMappedSmartDocumentsTemplateGroup
import net.atos.zac.smartdocuments.rest.toRestSmartDocumentsTemplateGroup
import net.atos.zac.smartdocuments.rest.toRestSmartDocumentsTemplateGroupSet
import net.atos.zac.smartdocuments.rest.toSmartDocumentsTemplateGroupSet
import net.atos.zac.smartdocuments.templates.model.SmartDocumentsTemplate
import net.atos.zac.smartdocuments.templates.model.SmartDocumentsTemplateGroup
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.util.UUID
import java.util.logging.Logger

@ApplicationScoped
@Transactional(SUPPORTS)
@NoArgConstructor
@AllOpen
class SmartDocumentsTemplatesService @Inject constructor(
    private val entityManager: EntityManager,
    private val smartDocumentsService: SmartDocumentsService,
    private val zaakafhandelParameterService: ZaakafhandelParameterService,
) {
    companion object {
        private val LOG = Logger.getLogger(DocumentCreationService::class.java.name)
    }

    /**
     * Lists all SmartDocuments template available
     */
    fun listTemplates() = smartDocumentsService.listTemplates().toRestSmartDocumentsTemplateGroupSet()

    /**
     * Stores template mapping for zaakafhandelparameters
     *
     * @param restTemplateGroups a set of RESTSmartDocumentsTemplateGroup objects to store
     * @param zaakafhandelParametersUUID UUID of the zaakafhandelparameters
     */
    @Transactional(REQUIRED)
    fun storeTemplatesMapping(
        restTemplateGroups: Set<RestMappedSmartDocumentsTemplateGroup>,
        zaakafhandelParametersUUID: UUID
    ) {
        LOG.info { "Storing template mapping for zaakafhandelParameters UUID $zaakafhandelParametersUUID" }

        val zaakafhandelParameters = zaakafhandelParameterService.readZaakafhandelParameters(zaakafhandelParametersUUID)
        val modelTemplateGroups = restTemplateGroups.toSmartDocumentsTemplateGroupSet(zaakafhandelParameters)

        deleteTemplateMapping(zaakafhandelParametersUUID)

        modelTemplateGroups.forEach { templateGroup ->
            entityManager.merge(templateGroup)
        }
    }

    /**
     * Deletes all template groups and templates for a zaakafhandelparameters
     *
     * @param zaakafhandelparametersUUID UUID of the zaakafhandelparameters
     * @return the number of entities deleted
     */
    @Transactional(REQUIRED)
    fun deleteTemplateMapping(
        zaakafhandelparametersUUID: UUID
    ): Int {
        LOG.info {
            "Deleting template mapping for zaakafhandelParameters UUID $zaakafhandelparametersUUID"
        }

        val zaakafhandelParametersId =
            zaakafhandelParameterService.readZaakafhandelParameters(zaakafhandelparametersUUID).id
        val builder = entityManager.criteriaBuilder
        val query = builder.createCriteriaDelete(SmartDocumentsTemplateGroup::class.java)
        val root = query.from(SmartDocumentsTemplateGroup::class.java)
        query.where(
            builder.equal(
                root.get<ZaakafhandelParameters>("zaakafhandelParameters").get<Long>("id"),
                zaakafhandelParametersId
            )
        )
        val deletedCount = entityManager.createQuery(query).executeUpdate()
        LOG.info { "Deleted $deletedCount template entities." }
        return deletedCount
    }

    /**
     * Lists all template groups for a zaakafhandelparameters
     *
     * @param zaakafhandelParametersUUID UUID of a zaakafhandelparameters
     * @return a set of all RESTSmartDocumentsTemplateGroup for the zaakafhandelparameters
     */
    fun getTemplatesMapping(
        zaakafhandelParametersUUID: UUID
    ): Set<RestMappedSmartDocumentsTemplateGroup> {
        LOG.info { "Fetching template mapping for zaakafhandelParameters UUID $zaakafhandelParametersUUID" }

        val zaakafhandelParametersId =
            zaakafhandelParameterService.readZaakafhandelParameters(zaakafhandelParametersUUID).id
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(SmartDocumentsTemplateGroup::class.java)
        val root = query.from(SmartDocumentsTemplateGroup::class.java)
        return entityManager.createQuery(
            query.select(root)
                .where(
                    builder.and(
                        builder.equal(
                            root.get<ZaakafhandelParameters>("zaakafhandelParameters").get<Long>("id"),
                            zaakafhandelParametersId
                        ),
                        builder.isNull(root.get<SmartDocumentsTemplateGroup>("parent"))
                    )
                )
        ).resultList.toSet().toRestSmartDocumentsTemplateGroup()
    }

    /**
     * Get the information object type UUID for a pair of group-template in a zaakafhandelparameters
     *
     * @param zaakafhandelParametersUUID UUID of a zaakafhandelparameters
     * @param templateGroupName name of a template group
     * @param templateName name of a template under the group
     * @return information object type UUID associated with this pair
     */
    fun getInformationObjectTypeUUID(
        zaakafhandelParametersUUID: UUID,
        templateGroupName: String,
        templateName: String
    ): UUID {
        LOG.info { "Fetching information object type UUID mapping for zaakafhandelParameters UUID " +
                "$zaakafhandelParametersUUID, template group $templateGroupName and template $templateName" }

        val zaakafhandelParametersId =
            zaakafhandelParameterService.readZaakafhandelParameters(zaakafhandelParametersUUID).id
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(SmartDocumentsTemplate::class.java)
        val root = query.from(SmartDocumentsTemplate::class.java)
        return entityManager.createQuery(
                query.select(root)
                    .where(
                        builder.and(
                            builder.equal(
                                root.get<ZaakafhandelParameters>("zaakafhandelParameters").get<Long>("id"),
                                zaakafhandelParametersId
                            ),
                            builder.equal(
                                root.get<SmartDocumentsTemplateGroup>("templateGroup").get<String>("name"),
                                templateGroupName
                            ),
                            builder.equal(
                                root.get<SmartDocumentsTemplate>("name"),
                                templateName
                            )
                        )
                    )
            ).singleResult.informatieObjectTypeUUID
    }
}
