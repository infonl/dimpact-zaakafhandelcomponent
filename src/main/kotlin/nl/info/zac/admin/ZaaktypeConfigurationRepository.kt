/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType.REQUIRED
import jakarta.transaction.Transactional.TxType.SUPPORTS
import nl.info.zac.admin.model.ProcessEngine
import nl.info.zac.admin.model.ZaakbeeindigReden
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.admin.model.ZaaktypeConfiguration.Companion.CREATIEDATUM_VARIABLE_NAME
import nl.info.zac.admin.model.ZaaktypeConfiguration.Companion.PRODUCTAANVRAAGTYPE_VARIABLE_NAME
import nl.info.zac.admin.model.ZaaktypeConfiguration.Companion.ZAAKTYPE_OMSCHRIJVING_VARIABLE_NAME
import nl.info.zac.admin.model.ZaaktypeConfiguration.Companion.ZAAKTYPE_UUID_VARIABLE_NAME
import nl.info.zac.admin.model.ZaaktypeDeadlineWarningWindows
import nl.info.zac.admin.model.ZaaktypeProcessBinding
import nl.info.zac.admin.model.ZaaktypeProcessBinding.Companion.DEFINITION_KEY_VARIABLE_NAME
import nl.info.zac.admin.model.ZaaktypeProcessBinding.Companion.PROCESS_ENGINE_VARIABLE_NAME
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Reads and writes zaaktype configurations. The current configuration of a zaaktype is the most recently created
 * configuration with its omschrijving.
 */
@ApplicationScoped
@Transactional(SUPPORTS)
@NoArgConstructor
@AllOpen
@Suppress("TooManyFunctions")
class ZaaktypeConfigurationRepository @Inject constructor(
    private val entityManager: EntityManager
) {
    fun findByZaaktypeUuid(zaaktypeUuid: UUID): ZaaktypeConfiguration? =
        query { criteriaBuilder, _, root ->
            listOf(criteriaBuilder.equal(root.get<UUID>(ZAAKTYPE_UUID_VARIABLE_NAME), zaaktypeUuid))
        }.firstOrNull()

    fun findCurrentByZaaktypeOmschrijving(zaaktypeOmschrijving: String): ZaaktypeConfiguration? =
        query { criteriaBuilder, _, root ->
            listOf(criteriaBuilder.equal(root.get<String>(ZAAKTYPE_OMSCHRIJVING_VARIABLE_NAME), zaaktypeOmschrijving))
        }.firstOrNull()

    /**
     * Returns the current configurations whose productaanvraagtype is the given one, the most recently created first.
     */
    fun listCurrentByProductaanvraagtype(productaanvraagtype: String): List<ZaaktypeConfiguration> =
        query { criteriaBuilder, query, root ->
            listOf(
                criteriaBuilder.equal(root.get<String>(PRODUCTAANVRAAGTYPE_VARIABLE_NAME), productaanvraagtype),
                isCurrent(criteriaBuilder, query, root)
            )
        }

    /**
     * Returns the configurations bound to the engine in the order in which they were stored.
     */
    fun listBoundTo(processEngine: ProcessEngine): List<ZaaktypeConfiguration> =
        query { criteriaBuilder, _, root ->
            listOf(
                criteriaBuilder.equal(
                    root.join<ZaaktypeConfiguration, ZaaktypeProcessBinding>(
                        ZaaktypeConfiguration::processBinding.name
                    ).get<ProcessEngine>(PROCESS_ENGINE_VARIABLE_NAME),
                    processEngine
                )
            )
        }.sortedBy { it.id }

    fun listDistinctDefinitionKeys(processEngine: ProcessEngine): List<String> {
        val criteriaBuilder = entityManager.criteriaBuilder
        val query = criteriaBuilder.createQuery(String::class.java)
        val root = query.from(ZaaktypeProcessBinding::class.java)
        query.select(root.get(DEFINITION_KEY_VARIABLE_NAME)).distinct(true)
            .where(criteriaBuilder.equal(root.get<ProcessEngine>(PROCESS_ENGINE_VARIABLE_NAME), processEngine))
        return entityManager.createQuery(query).resultList
    }

    /**
     * Returns the deadline warning windows of every zaaktype configuration that has at least one of them.
     * Only these three columns are read, so the configurations and their child rows are not loaded.
     */
    fun listDeadlineWarningWindows(): List<ZaaktypeDeadlineWarningWindows> {
        val criteriaBuilder = entityManager.criteriaBuilder
        val query = criteriaBuilder.createQuery(ZaaktypeDeadlineWarningWindows::class.java)
        val root = query.from(ZaaktypeConfiguration::class.java)
        val einddatumGeplandWaarschuwing = root.get<Int>(ZaaktypeConfiguration::einddatumGeplandWaarschuwing.name)
        val uiterlijkeEinddatumAfdoeningWaarschuwing =
            root.get<Int>(ZaaktypeConfiguration::uiterlijkeEinddatumAfdoeningWaarschuwing.name)
        query.select(
            criteriaBuilder.construct(
                ZaaktypeDeadlineWarningWindows::class.java,
                root.get<UUID>(ZAAKTYPE_UUID_VARIABLE_NAME),
                einddatumGeplandWaarschuwing,
                uiterlijkeEinddatumAfdoeningWaarschuwing
            )
        ).where(
            criteriaBuilder.or(
                criteriaBuilder.isNotNull(einddatumGeplandWaarschuwing),
                criteriaBuilder.isNotNull(uiterlijkeEinddatumAfdoeningWaarschuwing)
            )
        )
        return entityManager.createQuery(query).resultList
    }

    fun listZaakbeeindigRedenen(): List<ZaakbeeindigReden> {
        val criteriaBuilder = entityManager.criteriaBuilder
        val query = criteriaBuilder.createQuery(ZaakbeeindigReden::class.java)
        val root = query.from(ZaakbeeindigReden::class.java)
        query.orderBy(criteriaBuilder.asc(root.get<Any>("naam")))
        return entityManager.createQuery(query).resultList
    }

    @Transactional(REQUIRED)
    fun store(zaaktypeConfiguration: ZaaktypeConfiguration): ZaaktypeConfiguration =
        if (zaaktypeConfiguration.id == null) {
            entityManager.persist(zaaktypeConfiguration)
            zaaktypeConfiguration
        } else {
            entityManager.merge(zaaktypeConfiguration)
        }

    /**
     * Returns the configurations that match the predicates, the most recently created first.
     */
    private fun query(
        predicates: (CriteriaBuilder, CriteriaQuery<ZaaktypeConfiguration>, Root<ZaaktypeConfiguration>) -> List<Predicate>
    ): List<ZaaktypeConfiguration> {
        val criteriaBuilder = entityManager.criteriaBuilder
        val query: CriteriaQuery<ZaaktypeConfiguration> = criteriaBuilder.createQuery(ZaaktypeConfiguration::class.java)
        val root = query.from(ZaaktypeConfiguration::class.java)
        query.select(root)
            .where(predicates(criteriaBuilder, query, root))
            .orderBy(criteriaBuilder.desc(root.get<ZonedDateTime>(CREATIEDATUM_VARIABLE_NAME)))
        return entityManager.createQuery(query).resultList
    }

    private fun isCurrent(
        criteriaBuilder: CriteriaBuilder,
        query: CriteriaQuery<ZaaktypeConfiguration>,
        root: Root<ZaaktypeConfiguration>
    ): Predicate {
        val subquery = query.subquery(ZonedDateTime::class.java)
        val subqueryRoot = subquery.from(ZaaktypeConfiguration::class.java)
        subquery.select(criteriaBuilder.greatest(subqueryRoot.get<ZonedDateTime>(CREATIEDATUM_VARIABLE_NAME)))
            .where(
                criteriaBuilder.equal(
                    subqueryRoot.get<String>(ZAAKTYPE_OMSCHRIJVING_VARIABLE_NAME),
                    root.get<String>(ZAAKTYPE_OMSCHRIJVING_VARIABLE_NAME)
                )
            )
        return criteriaBuilder.equal(root.get<ZonedDateTime>(CREATIEDATUM_VARIABLE_NAME), subquery)
    }
}
