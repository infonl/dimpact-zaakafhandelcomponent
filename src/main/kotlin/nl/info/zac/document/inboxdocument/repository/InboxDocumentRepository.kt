/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.document.inboxdocument.repository

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType.REQUIRED
import jakarta.transaction.Transactional.TxType.SUPPORTS
import net.atos.client.zgw.shared.util.DateTimeUtil.convertToDateTime
import nl.info.zac.document.inboxdocument.repository.model.InboxDocument
import nl.info.zac.document.inboxdocument.repository.model.InboxDocumentListParameters
import nl.info.zac.search.model.DatumRange
import nl.info.zac.shared.model.SorteerRichting
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.Locale
import java.util.UUID

@ApplicationScoped
@Transactional(SUPPORTS)
@NoArgConstructor
@AllOpen
@Suppress("TooManyFunctions")
class InboxDocumentRepository @Inject constructor(
    private val entityManager: EntityManager
) {
    @Transactional(REQUIRED)
    fun save(inboxDocument: InboxDocument) = entityManager.persist(inboxDocument)

    fun find(id: Long): InboxDocument? = entityManager.find(InboxDocument::class.java, id)

    fun find(enkelvoudiginformatieobjectUUID: UUID): InboxDocument? {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(InboxDocument::class.java)
        val root = query.from(InboxDocument::class.java)
        query.select(root).where(
            builder.equal(
                root.get<UUID>(InboxDocument.ENKELVOUDIGINFORMATIEOBJECT_UUID_PROPERTY_NAME),
                enkelvoudiginformatieobjectUUID
            )
        )
        return entityManager.createQuery(query).resultList.firstOrNull()
    }

    @Transactional(REQUIRED)
    fun delete(inboxDocument: InboxDocument) = entityManager.remove(inboxDocument)

    fun count(listParameters: InboxDocumentListParameters): Int {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(Long::class.java)
        val root = query.from(InboxDocument::class.java)
        query.where(getWhere(listParameters = listParameters, root = root))
        query.select(builder.count(root))
        val count = entityManager.createQuery(query).singleResult ?: 0L
        return java.lang.Math.toIntExact(count)
    }

    fun list(listParameters: InboxDocumentListParameters): List<InboxDocument> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(InboxDocument::class.java)
        val root = query.from(InboxDocument::class.java)
        listParameters.sorting?.let { sorting ->
            if (sorting.direction == SorteerRichting.ASCENDING) {
                query.orderBy(builder.asc(root.get<Any>(sorting.field)))
            } else {
                query.orderBy(builder.desc(root.get<Any>(sorting.field)))
            }
        }
        query.where(getWhere(listParameters = listParameters, root = root))
        val emQuery = entityManager.createQuery(query)
        listParameters.paging?.let { paging ->
            emQuery.firstResult = paging.getFirstResult()
            emQuery.maxResults = paging.maxResults
        }
        return emQuery.resultList
    }

    private fun getWhere(listParameters: InboxDocumentListParameters, root: Root<InboxDocument>): Predicate {
        val builder = entityManager.criteriaBuilder
        val predicates = mutableListOf<Predicate>()
        listParameters.identificatie?.let {
            if (it.isNotBlank()) {
                predicates.add(
                    builder.like(
                        root.get(InboxDocument.ENKELVOUDIGINFORMATIEOBJECT_ID_PROPERTY_NAME),
                        "%$it%"
                    )
                )
            }
        }
        listParameters.titel?.let {
            if (it.isNotBlank()) {
                predicates.add(
                    builder.like(
                        builder.lower(root.get(InboxDocument.TITEL_PROPERTY_NAME)),
                        "%${it.lowercase(Locale.getDefault()).replace(" ", "%")}%"
                    )
                )
            }
        }
        addCreatiedatumPredicates(
            creatiedatum = listParameters.creatiedatum,
            predicates = predicates,
            root = root,
            builder = builder
        )
        @Suppress("SpreadOperator")
        return builder.and(*predicates.toTypedArray())
    }

    private fun addCreatiedatumPredicates(
        creatiedatum: DatumRange?,
        predicates: MutableList<Predicate>,
        root: Root<InboxDocument>,
        builder: CriteriaBuilder
    ) {
        creatiedatum?.let {
            it.van?.let { van ->
                predicates.add(
                    builder.greaterThanOrEqualTo(
                        root.get(InboxDocument.CREATIE_DATUM_PROPERTY_NAME),
                        convertToDateTime(van)
                    )
                )
            }
            it.tot?.let { tot ->
                predicates.add(
                    builder.lessThanOrEqualTo(
                        root.get(InboxDocument.CREATIE_DATUM_PROPERTY_NAME),
                        convertToDateTime(tot).plusDays(1).minusSeconds(1)
                    )
                )
            }
        }
    }
}
