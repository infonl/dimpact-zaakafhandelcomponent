/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.smartdocuments

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType.REQUIRED
import jakarta.transaction.Transactional.TxType.SUPPORTS
import net.atos.zac.documentcreatie.DocumentCreatieService
import net.atos.zac.smartdocuments.rest.RESTSmartDocumentsTemplateGroup
import net.atos.zac.smartdocuments.templates.SmartDocumentsTemplateConverter.toModel
import net.atos.zac.smartdocuments.templates.SmartDocumentsTemplateConverter.toREST
import net.atos.zac.smartdocuments.templates.SmartDocumentsTemplateConverter.toStringRepresentation
import net.atos.zac.smartdocuments.templates.model.SmartDocumentsTemplateGroup
import net.atos.zac.zaaksturing.ZaakafhandelParameterService
import net.atos.zac.zaaksturing.model.ZaakafhandelParameters
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.util.UUID
import java.util.logging.Logger

@ApplicationScoped
@Transactional(SUPPORTS)
@NoArgConstructor
@AllOpen
class SmartDocumentsService @Inject constructor(
    private val documentCreatieService: DocumentCreatieService,
    private var zaakafhandelParameterService: ZaakafhandelParameterService,
) {
    companion object {
        private val LOG = Logger.getLogger(SmartDocumentsService::class.java.name)
    }

    @PersistenceContext(unitName = "ZaakafhandelcomponentPU")
    private lateinit var entityManager: EntityManager

    /**
     * Lists all SmartDocuments template available
     */
    fun listTemplates() = documentCreatieService.listTemplates().toREST()

    /**
     * Stores template mapping for ZaakafhandelParameters
     *
     * @param restTemplateGroups a set of RESTSmartDocumentsTemplateGroup objects to store
     * @param zaakafhandelUUID UUID of the ZaakafhandelParameters
     */
    @Transactional(REQUIRED)
    fun storeTemplatesMapping(restTemplateGroups: Set<RESTSmartDocumentsTemplateGroup>, zaakafhandelUUID: UUID) {
        LOG.info { "Storing template mapping for zaakafhandelParameters UUID $zaakafhandelUUID" }

        val zaakafhandelParameters = zaakafhandelParameterService.readZaakafhandelParameters(zaakafhandelUUID)
        val modelTemplateGroups = restTemplateGroups.toModel(zaakafhandelParameters)

        deleteTemplateMapping(zaakafhandelUUID)

        modelTemplateGroups.forEach { templateGroup ->
            entityManager.merge(templateGroup)
        }
    }

    /**
     * Deletes all template groups and templates for a ZaakafhandelParameters
     *
     * @param zaakafhandelUUID UUID of the ZaakafhandelParameters
     * @return the number of entities deleted
     */
    @Transactional(REQUIRED)
    fun deleteTemplateMapping(zaakafhandelUUID: UUID): Int {
        LOG.info("Deleting template mapping for zaakafhandelParameters UUID $zaakafhandelUUID")

        val zaakafhandelParametersId = zaakafhandelParameterService.readZaakafhandelParameters(zaakafhandelUUID).id
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
        LOG.info("Deleted $deletedCount template entities.")
        return deletedCount
    }

    /**
     * Lists all template groups for a ZaakafhandelParameters
     *
     * @param zaakafhandelUUID UUID of a ZaakafhandelParameters
     * @return a set of all RESTSmartDocumentsTemplateGroup for the ZaakafhandelParameters
     */
    fun getTemplatesMapping(zaakafhandelUUID: UUID): Set<RESTSmartDocumentsTemplateGroup> {
        LOG.info { "Fetching template mapping for zaakafhandelParameters UUID $zaakafhandelUUID" }

        val zaakafhandelParametersId = zaakafhandelParameterService.readZaakafhandelParameters(zaakafhandelUUID).id
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(
            SmartDocumentsTemplateGroup::class.java
        )
        val root = query.from(
            SmartDocumentsTemplateGroup::class.java
        )
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
        ).resultList.toSet().toREST()
    }
}

/**
 * Validates that all elements in a RESTSmartDocumentsTemplateGroup set are part of  pre-defined
 * RESTSmartDocumentsTemplateGroup superset.
 * The superset can be returned by SmartDocuments structure API or stored in our DB
 *
 * @param supersetTemplates set of RESTSmartDocumentsTemplateGroup to validate against
 */
fun Set<RESTSmartDocumentsTemplateGroup>.validate(
    supersetTemplates: Set<RESTSmartDocumentsTemplateGroup>
) {
    val superset = supersetTemplates.toStringRepresentation()
    val subset = this.toStringRepresentation()

    val errors = subset.filterNot { superset.contains(it) }
    if (errors.isNotEmpty()) {
        throw SmartDocumentsException("Validation failed. Unknown entities: $errors")
    }
}
