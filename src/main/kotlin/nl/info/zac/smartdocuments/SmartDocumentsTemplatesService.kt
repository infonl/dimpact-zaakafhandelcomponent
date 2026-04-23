/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.smartdocuments

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType.REQUIRED
import jakarta.transaction.Transactional.TxType.SUPPORTS
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.documentcreation.CmmnDocumentCreationService
import nl.info.zac.smartdocuments.exception.SmartDocumentsConfigurationException
import nl.info.zac.smartdocuments.rest.RestMappedSmartDocumentsTemplateGroup
import nl.info.zac.smartdocuments.rest.RestSmartDocumentsTemplateGroup
import nl.info.zac.smartdocuments.rest.group
import nl.info.zac.smartdocuments.rest.toRestSmartDocumentsTemplateGroup
import nl.info.zac.smartdocuments.rest.toRestSmartDocumentsTemplateGroupSet
import nl.info.zac.smartdocuments.rest.toSmartDocumentsTemplateGroupSet
import nl.info.zac.smartdocuments.templates.model.SmartDocumentsTemplate
import nl.info.zac.smartdocuments.templates.model.SmartDocumentsTemplateGroup
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID
import java.util.logging.Logger

@ApplicationScoped
@Transactional(SUPPORTS)
@NoArgConstructor
@AllOpen
@Suppress("TooManyFunctions")
class SmartDocumentsTemplatesService @Inject constructor(
    private val entityManager: EntityManager,
    private val smartDocumentsService: SmartDocumentsService,
    private val zaaktypeConfigurationService: ZaaktypeConfigurationService,
) {
    companion object {
        private val LOG = Logger.getLogger(CmmnDocumentCreationService::class.java.name)
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
     * Lists all SmartDocuments template names for a template group.
     *
     * @param groupPath path to the template group, starting with the root group.
     * @return A list of template names in the group
     */
    fun listGroupTemplateNames(groupPath: List<String>) =
        if (smartDocumentsService.isEnabled()) {
            listTemplates().group(groupPath).templates?.map { it.name } ?: emptyList()
        } else {
            emptyList()
        }

    /**
     * Return SmartDocuments template group data
     *
     * @param groupPath path to the template group, starting with the root group.
     * @return A list of template names in the group
     */
    fun getTemplateGroup(groupPath: List<String>): RestSmartDocumentsTemplateGroup =
        if (smartDocumentsService.isEnabled()) {
            listTemplates().group(groupPath)
        } else {
            throw SmartDocumentsConfigurationException("Smart documents is disabled")
        }

    /**
     * Stores template mapping for zaaktypeConfiguration
     *
     * @param restTemplateGroups a set of RESTSmartDocumentsTemplateGroup objects to store
     * @param zaaktypeUUID UUID of the zaaktype
     */
    @Transactional(REQUIRED)
    fun storeTemplatesMapping(
        restTemplateGroups: Set<RestMappedSmartDocumentsTemplateGroup>,
        zaaktypeUUID: UUID
    ) {
        LOG.fine { "Storing template mapping for zaaktype UUID $zaaktypeUUID" }

        requireNotNull(zaaktypeConfigurationService.readZaaktypeConfiguration(zaaktypeUUID)) {
            "No zaaktype configuration found for zaaktype UUID $zaaktypeUUID"
        }.let {
            restTemplateGroups.toSmartDocumentsTemplateGroupSet(it).let { modelTemplateGroups ->
                deleteTemplateMapping(zaaktypeUUID)
                modelTemplateGroups.forEach { templateGroup ->
                    entityManager.merge(templateGroup)
                }
            }
        }
    }

    private fun getZaaktypeConfigurationId(zaaktypeUUID: UUID): Long? =
        zaaktypeConfigurationService.readZaaktypeConfiguration(zaaktypeUUID)?.id

    /**
     * Deletes all template groups and templates for a zaaktypeConfiguration
     *
     * @param zaaktypeUUID UUID of the zaaktype
     * @return the number of entities deleted
     */
    @Transactional(REQUIRED)
    fun deleteTemplateMapping(
        zaaktypeUUID: UUID
    ): Int {
        LOG.fine { "Deleting template mapping for zaaktype UUID $zaaktypeUUID" }

        entityManager.criteriaBuilder.let { builder ->
            builder.createCriteriaDelete(SmartDocumentsTemplateGroup::class.java).let { query ->
                query.from(SmartDocumentsTemplateGroup::class.java).let { root ->
                    query.where(
                        builder.equal(
                            root.get<ZaaktypeConfiguration>(SmartDocumentsTemplateGroup::zaaktypeConfiguration.name)
                                .get<Long>("id"),
                            getZaaktypeConfigurationId(zaaktypeUUID)
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
     * Lists all template groups for a zaaktypeConfiguration
     *
     * @param zaaktypeUUID UUID of a zaaktype
     * @return a set of all RESTSmartDocumentsTemplateGroup for the zaaktypeConfiguration
     */
    fun getTemplatesMapping(
        zaaktypeUUID: UUID
    ): Set<RestMappedSmartDocumentsTemplateGroup> =
        if (!smartDocumentsService.isEnabled()) {
            LOG.fine { "Smart documents is disabled. Returning empty set of template groups" }
            emptySet()
        } else if (zaaktypeConfigurationService.readZaaktypeConfiguration(zaaktypeUUID) == null) {
            // A zaaktype configuration is only persisted after first save — return empty set rather than querying with a null id
            LOG.fine { "No zaaktype configuration found for zaaktype UUID $zaaktypeUUID. Returning empty set of template groups" }
            emptySet()
        } else {
            LOG.fine { "Fetching template mapping for zaaktype UUID $zaaktypeUUID" }
            fetchTemplatesMapping(zaaktypeUUID)
        }

    private fun fetchTemplatesMapping(zaaktypeUUID: UUID): Set<RestMappedSmartDocumentsTemplateGroup> =
        entityManager.criteriaBuilder.let { builder ->
            builder.createQuery(SmartDocumentsTemplateGroup::class.java).let { query ->
                query.from(SmartDocumentsTemplateGroup::class.java).let { root ->
                    return entityManager.createQuery(
                        query.select(root)
                            .where(
                                builder.and(
                                    builder.equal(
                                        root.get<ZaaktypeConfiguration>(
                                            SmartDocumentsTemplateGroup::zaaktypeConfiguration.name
                                        )
                                            .get<Long>("id"),
                                        getZaaktypeConfigurationId(zaaktypeUUID)
                                    ),
                                    builder.isNull(root.get<SmartDocumentsTemplateGroup>("parent"))
                                )
                            )
                    ).resultList.toSet().toRestSmartDocumentsTemplateGroup()
                }
            }
        }

    /**
     * Get the information object type UUID for a pair of group-template in a zaaktypeConfiguration
     *
     * @param zaaktypeUUID UUID of a zaaktype
     * @param templateGroupId name of a template group
     * @param templateId name of a template under the group
     * @return information object type UUID associated with this pair
     */
    @Suppress("NestedBlockDepth")
    fun getInformationObjectTypeUUID(
        zaaktypeUUID: UUID,
        templateGroupId: String,
        templateId: String
    ): UUID {
        LOG.fine {
            "Fetching information object type UUID mapping for zaaktype UUID " +
                "$zaaktypeUUID, template group id $templateGroupId and template id $templateId"
        }

        return entityManager.criteriaBuilder.let { builder ->
            builder.createTupleQuery().let { criteriaQuery ->
                criteriaQuery.from(SmartDocumentsTemplate::class.java).let { root ->
                    root.get<UUID>(SmartDocumentsTemplate::informatieObjectTypeUUID.name).let { namePath ->
                        criteriaQuery.multiselect(namePath).where(
                            builder.and(
                                builder.equal(
                                    root.get<ZaaktypeConfiguration>(
                                        SmartDocumentsTemplate::zaaktypeConfiguration.name
                                    )
                                        .get<Long>("id"),
                                    getZaaktypeConfigurationId(zaaktypeUUID)
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
                        }.takeIf { it != null } ?: throw SmartDocumentsConfigurationException(
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
                        }.takeIf { it != null } ?: throw SmartDocumentsConfigurationException(
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
                        }.takeIf { it != null } ?: throw SmartDocumentsConfigurationException(
                            "Template with id $templateId is not configured"
                        )
                    }
                }
            }
        }
    }
}
