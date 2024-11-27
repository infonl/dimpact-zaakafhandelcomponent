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
    fun listTemplates() =
        if (smartDocumentsService.isEnabled()) {
            smartDocumentsService.listTemplates().toRestSmartDocumentsTemplateGroupSet()
        } else {
            emptySet()
        }

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
        LOG.fine { "Storing template mapping for zaakafhandelParameters UUID $zaakafhandelParametersUUID" }

        zaakafhandelParameterService.readZaakafhandelParameters(zaakafhandelParametersUUID).let {
            restTemplateGroups.toSmartDocumentsTemplateGroupSet(it).let { modelTemplateGroups ->
                deleteTemplateMapping(zaakafhandelParametersUUID)
                modelTemplateGroups.forEach { templateGroup ->
                    entityManager.merge(templateGroup)
                }
            }
        }
    }

    private fun getZaakafhandelParametersId(zaakafhandelParametersUUID: UUID) =
        zaakafhandelParameterService.readZaakafhandelParameters(zaakafhandelParametersUUID).id

    /**
     * Deletes all template groups and templates for a zaakafhandelparameters
     *
     * @param zaakafhandelParametersUUID UUID of the zaakafhandelparameters
     * @return the number of entities deleted
     */
    @Transactional(REQUIRED)
    fun deleteTemplateMapping(
        zaakafhandelParametersUUID: UUID
    ): Int {
        LOG.fine { "Deleting template mapping for zaakafhandelParameters UUID $zaakafhandelParametersUUID" }

        entityManager.criteriaBuilder.let { builder ->
            builder.createCriteriaDelete(SmartDocumentsTemplateGroup::class.java).let { query ->
                query.from(SmartDocumentsTemplateGroup::class.java).let { root ->
                    query.where(
                        builder.equal(
                            root.get<ZaakafhandelParameters>(SmartDocumentsTemplate::zaakafhandelParameters.name)
                                .get<Long>("id"),
                            getZaakafhandelParametersId(zaakafhandelParametersUUID)
                        )
                    )
                    return entityManager.createQuery(query).executeUpdate().also {
                        LOG.info { "Deleted $it template entities." }
                    }
                }
            }
        }
    }

    /**
     * Lists all template groups for a zaakafhandelparameters
     *
     * @param zaakafhandelParametersUUID UUID of a zaakafhandelparameters
     * @return a set of all RESTSmartDocumentsTemplateGroup for the zaakafhandelparameters
     */
    fun getTemplatesMapping(
        zaakafhandelParametersUUID: UUID
    ): Set<RestMappedSmartDocumentsTemplateGroup> =
        if (!smartDocumentsService.isEnabled()) {
            LOG.fine { "Smart documents is disabled. Returning empty set of template groups" }
            emptySet()
        } else {
            LOG.fine { "Fetching template mapping for zaakafhandelParameters UUID $zaakafhandelParametersUUID" }
            fetchTemplatesMapping(zaakafhandelParametersUUID)
        }

    private fun fetchTemplatesMapping(zaakafhandelParametersUUID: UUID): Set<RestMappedSmartDocumentsTemplateGroup> =
        entityManager.criteriaBuilder.let { builder ->
            builder.createQuery(SmartDocumentsTemplateGroup::class.java).let { query ->
                query.from(SmartDocumentsTemplateGroup::class.java).let { root ->
                    return entityManager.createQuery(
                        query.select(root)
                            .where(
                                builder.and(
                                    builder.equal(
                                        root.get<ZaakafhandelParameters>(
                                            SmartDocumentsTemplateGroup::zaakafhandelParameters.name
                                        )
                                            .get<Long>("id"),
                                        getZaakafhandelParametersId(zaakafhandelParametersUUID)
                                    ),
                                    builder.isNull(root.get<SmartDocumentsTemplateGroup>("parent"))
                                )
                            )
                    ).resultList.toSet().toRestSmartDocumentsTemplateGroup()
                }
            }
        }

    /**
     * Get the information object type UUID for a pair of group-template in a zaakafhandelparameters
     *
     * @param zaakafhandelParametersUUID UUID of a zaakafhandelparameters
     * @param templateGroupId name of a template group
     * @param templateId name of a template under the group
     * @return information object type UUID associated with this pair
     */
    @Suppress("NestedBlockDepth")
    fun getInformationObjectTypeUUID(
        zaakafhandelParametersUUID: UUID,
        templateGroupId: String,
        templateId: String
    ): UUID {
        LOG.fine {
            "Fetching information object type UUID mapping for zaakafhandelParameters UUID " +
                "$zaakafhandelParametersUUID, template group id $templateGroupId and template id $templateId"
        }

        return entityManager.criteriaBuilder.let { builder ->
            builder.createTupleQuery().let { criteriaQuery ->
                criteriaQuery.from(SmartDocumentsTemplate::class.java).let { root ->
                    root.get<UUID>(SmartDocumentsTemplate::informatieObjectTypeUUID.name).let { namePath ->
                        criteriaQuery.multiselect(namePath).where(
                            builder.and(
                                builder.equal(
                                    root.get<ZaakafhandelParameters>(
                                        SmartDocumentsTemplate::zaakafhandelParameters.name
                                    )
                                        .get<Long>("id"),
                                    getZaakafhandelParametersId(zaakafhandelParametersUUID)
                                ),
                                builder.equal(
                                    root.get<SmartDocumentsTemplateGroup>(
                                        SmartDocumentsTemplate::templateGroup.name
                                    )
                                        .get<String>(SmartDocumentsTemplate::smartDocumentsId.name),
                                    templateGroupId
                                ),
                                builder.equal(
                                    root.get<SmartDocumentsTemplate>(
                                        SmartDocumentsTemplate::smartDocumentsId.name
                                    ),
                                    templateId
                                )
                            )
                        ).let { multiselectQuery ->
                            entityManager.createQuery(multiselectQuery)
                                .setMaxResults(1)
                                .resultList.firstOrNull()
                                ?.get(namePath)
                        }.takeIf { it != null } ?: throw SmartDocumentsException(
                            "No information object type mapped for template group id " +
                                "$templateGroupId and template id $templateId"
                        )
                    }
                }
            }
        }
    }

    /**
     * Get the template group name
     *
     * @param templateGroupId SmartDocuments' id of a template group
     * @return template group name
     */
    @Suppress("NestedBlockDepth")
    fun getTemplateGroupName(templateGroupId: String): String {
        LOG.fine { "Fetching template group name for id $templateGroupId" }

        return entityManager.criteriaBuilder.let { builder ->
            builder.createTupleQuery().let { criteriaQuery ->
                criteriaQuery.from(SmartDocumentsTemplateGroup::class.java).let { root ->
                    root.get<String>(SmartDocumentsTemplateGroup::name.name).let { namePath ->
                        criteriaQuery.multiselect(namePath).where(
                            builder.equal(
                                root.get<String>(SmartDocumentsTemplateGroup::smartDocumentsId.name),
                                templateGroupId
                            )
                        ).let { multiselectQuery ->
                            entityManager.createQuery(multiselectQuery)
                                .setMaxResults(1)
                                .resultList.firstOrNull()
                                ?.get(namePath)
                        }.takeIf { it != null } ?: throw SmartDocumentsException(
                            "Template group with id $templateGroupId is not configured"
                        )
                    }
                }
            }
        }
    }

    /**
     * Get the template name
     *
     * @param templateId SmartDocuments' id of a template
     * @return template name
     */
    @Suppress("NestedBlockDepth")
    fun getTemplateName(templateId: String): String {
        LOG.fine { "Fetching template group name for id $templateId" }

        return entityManager.criteriaBuilder.let { builder ->
            builder.createTupleQuery().let { criteriaQuery ->
                criteriaQuery.from(SmartDocumentsTemplate::class.java).let { root ->
                    root.get<String>(SmartDocumentsTemplate::name.name).let { namePath ->
                        criteriaQuery.multiselect(namePath).where(
                            builder.equal(
                                root.get<String>(SmartDocumentsTemplate::smartDocumentsId.name),
                                templateId
                            )
                        ).let { multiselectQuery ->
                            entityManager.createQuery(multiselectQuery)
                                .setMaxResults(1)
                                .resultList.firstOrNull()
                                ?.get(namePath)
                        }.takeIf { it != null } ?: throw SmartDocumentsException(
                            "Template with id $templateId is not configured"
                        )
                    }
                }
            }
        }
    }
}
