/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.document.inboxdocument

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import jakarta.transaction.Transactional
import jakarta.ws.rs.NotFoundException
import net.atos.client.zgw.shared.util.DateTimeUtil.convertToDateTime
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.document.inboxdocument.model.InboxDocument
import nl.info.zac.document.inboxdocument.model.InboxDocumentListParameters
import nl.info.zac.search.model.DatumRange
import nl.info.zac.shared.model.SorteerRichting
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.UUID

@ApplicationScoped
@Transactional
@NoArgConstructor
@AllOpen
@Suppress("TooManyFunctions")
class InboxDocumentService @Inject constructor(
    private val entityManager: EntityManager,
    private val zrcClientService: ZrcClientService,
    private val drcClientService: DrcClientService
) {
    fun create(enkelvoudiginformatieobjectUUID: UUID): InboxDocument {
        val informatieobject = drcClientService.readEnkelvoudigInformatieobject(enkelvoudiginformatieobjectUUID)
        return InboxDocument().apply {
            enkelvoudiginformatieobjectID = informatieobject.identificatie
            this.enkelvoudiginformatieobjectUUID = enkelvoudiginformatieobjectUUID
            creatiedatum = informatieobject.creatiedatum
            titel = informatieobject.titel
            bestandsnaam = informatieobject.bestandsnaam
        }.also { entityManager.persist(it) }
    }

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

    fun read(enkelvoudiginformatieobjectUUID: UUID): InboxDocument =
        find(enkelvoudiginformatieobjectUUID)
            ?: throw NotFoundException("InboxDocument with uuid '$enkelvoudiginformatieobjectUUID' not found.")

    fun count(listParameters: InboxDocumentListParameters): Int {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(Long::class.java)
        val root = query.from(InboxDocument::class.java)
        query.where(getWhere(listParameters = listParameters, root = root))
        query.select(builder.count(root))
        return entityManager.createQuery(query).singleResult?.toInt() ?: 0
    }

    fun delete(id: Long) {
        find(id)?.let { entityManager.remove(it) }
    }

    fun delete(uuid: UUID) {
        find(uuid)?.let { entityManager.remove(it) }
    }

    fun deleteForZaakinformatieobject(zaakinformatieobjectUUID: UUID) {
        val zaakInformatieobject = zrcClientService.readZaakinformatieobject(zaakinformatieobjectUUID)
        find(zaakInformatieobject.informatieobject.extractUuid())?.let { entityManager.remove(it) }
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
        if (!listParameters.identificatie.isNullOrBlank()) {
            predicates.add(
                builder.like(
                    root.get(InboxDocument.ENKELVOUDIGINFORMATIEOBJECT_ID_PROPERTY_NAME),
                    "%${listParameters.identificatie}%"
                )
            )
        }
        if (!listParameters.titel.isNullOrBlank()) {
            val titel = listParameters.titel!!
            predicates.add(
                builder.like(
                    builder.lower(root.get(InboxDocument.TITEL_PROPERTY_NAME)),
                    "%${titel.lowercase().replace(" ", "%")}%"
                )
            )
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
