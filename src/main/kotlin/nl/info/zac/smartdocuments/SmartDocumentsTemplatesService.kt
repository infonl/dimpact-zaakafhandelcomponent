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
import nl.info.client.smartdocuments.model.document.Selection
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.smartdocuments.exception.SmartDocumentsConfigurationException
import nl.info.zac.smartdocuments.rest.RestMappedSmartDocumentsTemplateGroup
import nl.info.zac.smartdocuments.rest.RestSmartDocumentsTemplateGroup
import nl.info.zac.smartdocuments.rest.findGroupById
import nl.info.zac.smartdocuments.rest.findTemplateById
import nl.info.zac.smartdocuments.rest.group
import nl.info.zac.smartdocuments.rest.resolveCurrentNames
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
        private val LOG = Logger.getLogger(SmartDocumentsTemplatesService::class.java.name)
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
     * Copies SmartDocuments template mappings from a previous zaaktype to a new zaaktype.
     *
     * @param previousZaaktypeUuid UUID of the zaaktype to copy mappings from
     * @param newZaaktypeUuid UUID of the zaaktype to copy mappings to
     */
    @Transactional(REQUIRED)
    fun copySmartDocumentsTemplateMappings(previousZaaktypeUuid: UUID, newZaaktypeUuid: UUID) {
        val templateMappings = getTemplatesMapping(previousZaaktypeUuid)
        storeTemplatesMapping(templateMappings, newZaaktypeUuid)
    }

    /**
     * Lists all template groups for a zaaktypeConfiguration
     *
     * @param zaaktypeUuid UUID of a zaaktype
     * @return a set of all RESTSmartDocumentsTemplateGroup for the zaaktypeConfiguration
     */
    fun getTemplatesMapping(
        zaaktypeUuid: UUID
    ): Set<RestMappedSmartDocumentsTemplateGroup> =
        if (!smartDocumentsService.isEnabled()) {
            LOG.fine { "Smart documents is disabled. Returning empty set of template groups" }
            emptySet()
        } else if (zaaktypeConfigurationService.readZaaktypeConfiguration(zaaktypeUuid) == null) {
            // A zaaktype configuration is only persisted after first save — return empty set rather than querying with a null id
            LOG.fine { "No zaaktype configuration found for zaaktype UUID '$zaaktypeUuid'. Returning empty set of template groups" }
            emptySet()
        } else {
            LOG.fine { "Fetching template mapping for zaaktype UUID $zaaktypeUuid" }
            fetchTemplatesMapping(zaaktypeUuid)
        }

    private fun fetchTemplatesMapping(zaaktypeUUID: UUID): Set<RestMappedSmartDocumentsTemplateGroup> =
        entityManager.criteriaBuilder.let { builder ->
            builder.createQuery(SmartDocumentsTemplateGroup::class.java).let { query ->
                query.from(SmartDocumentsTemplateGroup::class.java).let { root ->
                    entityManager.createQuery(
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
                    ).resultList.toSet()
                }
            }
        }.toRestSmartDocumentsTemplateGroup().let { persistedMapping ->
            if (persistedMapping.isEmpty()) persistedMapping else persistedMapping.resolveCurrentNames(listTemplates())
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
            builder.createQuery(UUID::class.java).let { criteriaQuery ->
                criteriaQuery.from(SmartDocumentsTemplate::class.java).let { root ->
                    criteriaQuery.select(
                        root.get(SmartDocumentsTemplate::informatieObjectTypeUUID.name)
                    ).where(
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
                                    .get<String>(SmartDocumentsTemplateGroup::smartDocumentsId.name),
                                templateGroupId
                            ),
                            builder.equal(
                                root.get<String>(
                                    SmartDocumentsTemplate::smartDocumentsId.name
                                ),
                                templateId
                            )
                        )
                    ).let { selectQuery ->
                        entityManager.createQuery(selectQuery)
                            .setMaxResults(1)
                            .resultList.firstOrNull()
                    } ?: throw SmartDocumentsConfigurationException(
                        "No information object type mapped for template group id " +
                            "$templateGroupId and template id $templateId"
                    )
                }
            }
        }
    }

    /**
     * Resolves the current template group name and template name directly from SmartDocuments, matched by id,
     * instead of the name persisted in ZAC's own database, and returns them as a ready-to-send [Selection].
     * Both names are resolved from a single live SmartDocuments read, since a persisted name can go stale
     * the moment either is renamed in SmartDocuments.
     *
     * @param templateGroupId SmartDocuments' id of a template group
     * @param templateId SmartDocuments' id of a template
     * @return a [Selection] holding the current template group name and template name
     * @throws SmartDocumentsConfigurationException when either id no longer exists in SmartDocuments
     */
    fun readCurrentSelection(templateGroupId: String, templateId: String): Selection =
        listTemplates().let { currentTemplateGroups ->
            Selection(
                templateGroup = currentTemplateGroups.findGroupById(templateGroupId)?.name
                    ?: throw SmartDocumentsConfigurationException(
                        "Template group with id $templateGroupId no longer exists in SmartDocuments"
                    ),
                template = currentTemplateGroups.findTemplateById(templateId)?.name
                    ?: throw SmartDocumentsConfigurationException(
                        "Template with id $templateId no longer exists in SmartDocuments"
                    )
            )
        }
}
