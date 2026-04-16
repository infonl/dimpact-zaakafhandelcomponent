/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.productaanvraag

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import jakarta.transaction.Transactional
import net.atos.zac.productaanvraag.model.InboxProductaanvraag
import net.atos.zac.productaanvraag.model.InboxProductaanvraagListParameters
import net.atos.zac.productaanvraag.model.InboxProductaanvraagResultaat
import nl.info.zac.shared.model.SorteerRichting
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.Optional
import java.util.logging.Logger

@ApplicationScoped
@Transactional
@AllOpen
@NoArgConstructor
class InboxProductaanvraagService @Inject constructor(
    private val entityManager: EntityManager
) {
    companion object {
        private const val LIKE = "%%%s%%"
        private val LOG = Logger.getLogger(InboxProductaanvraagService::class.java.name)
    }

    fun create(inboxProductaanvraag: InboxProductaanvraag) {
        entityManager.persist(inboxProductaanvraag)
    }

    fun list(listParameters: InboxProductaanvraagListParameters): InboxProductaanvraagResultaat =
        InboxProductaanvraagResultaat(query(listParameters), count(listParameters), listTypes(listParameters))

    fun delete(id: Long?) {
        id?.let { find(it).ifPresent(entityManager::remove) }
    }

    fun find(id: Long): Optional<InboxProductaanvraag> {
        val inboxProductaanvraag = entityManager.find(InboxProductaanvraag::class.java, id)
        return if (inboxProductaanvraag != null) Optional.of(inboxProductaanvraag) else Optional.empty()
    }

    private fun query(listParameters: InboxProductaanvraagListParameters): List<InboxProductaanvraag> {
        LOG.info("Querying inbox productaanvragen with parameters: $listParameters")
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(InboxProductaanvraag::class.java)
        val root = query.from(InboxProductaanvraag::class.java)
        listParameters.sorting?.let { sorting ->
            if (sorting.direction == SorteerRichting.ASCENDING) {
                query.orderBy(builder.asc(root.get<Any>(sorting.field)))
            } else {
                query.orderBy(builder.desc(root.get<Any>(sorting.field)))
            }
        }
        query.where(getWhere(listParameters, root))
        val emQuery = entityManager.createQuery(query)
        listParameters.paging?.let { paging ->
            emQuery.firstResult = paging.getFirstResult()
            emQuery.maxResults = paging.maxResults
        }
        return emQuery.resultList
    }

    private fun count(listParameters: InboxProductaanvraagListParameters): Long {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(Long::class.javaObjectType)
        val root = query.from(InboxProductaanvraag::class.java)
        query.select(builder.count(root))
        query.where(getWhere(listParameters, root))
        return entityManager.createQuery(query).singleResult ?: 0L
    }

    private fun listTypes(listParameters: InboxProductaanvraagListParameters): List<String> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(String::class.java)
        val root = query.from(InboxProductaanvraag::class.java)
        query.select(root.get<String>(InboxProductaanvraag.TYPE)).distinct(true)
        query.where(getWhere(listParameters, root))
        return entityManager.createQuery(query).resultList
    }

    private fun getWhere(
        listParameters: InboxProductaanvraagListParameters,
        root: Root<InboxProductaanvraag>
    ): Predicate {
        val builder = entityManager.criteriaBuilder
        val predicates = mutableListOf<Predicate>()
        listParameters.initiatorID?.takeIf { it.isNotBlank() }?.let {
            predicates.add(builder.like(root.get(InboxProductaanvraag.INITIATOR), LIKE.format(it)))
        }
        listParameters.type?.takeIf { it.isNotBlank() }?.let {
            predicates.add(builder.equal(root.get<String>(InboxProductaanvraag.TYPE), it))
        }
        listParameters.ontvangstdatum?.let { range ->
            range.van?.let {
                predicates.add(builder.greaterThanOrEqualTo(root.get(InboxProductaanvraag.ONTVANGSTDATUM), it))
            }
            range.tot?.let {
                predicates.add(builder.lessThanOrEqualTo(root.get(InboxProductaanvraag.ONTVANGSTDATUM), it))
            }
        }
        @Suppress("SpreadOperator")
        return builder.and(*predicates.toTypedArray())
    }
}
