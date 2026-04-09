/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.document.detacheddocument.repository

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import jakarta.transaction.Transactional
import jakarta.transaction.Transactional.TxType.REQUIRED
import jakarta.transaction.Transactional.TxType.SUPPORTS
import net.atos.client.zgw.shared.util.DateTimeUtil
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.zac.app.informatieobjecten.exception.DetachedDocumentNotFoundException
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocument
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocument.Companion.REDEN_PROPERTY_NAME
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocument.Companion.TITEL_PROPERTY_NAME
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocument.Companion.ZAAK_ID_PROPERTY_NAME
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocumentListParameters
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocumentResult
import nl.info.zac.search.model.DatumRange
import nl.info.zac.shared.model.SorteerRichting
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.apache.commons.lang3.StringUtils
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID

@ApplicationScoped
@Transactional(SUPPORTS)
@NoArgConstructor
@AllOpen
@Suppress("TooManyFunctions")
class DetachedDocumentRepository @Inject constructor(
    private val entityManager: EntityManager,
    private val loggedInUserInstance: Instance<LoggedInUser>
) {
    companion object {
        private const val LIKE = "%%%s%%"
    }

    @Transactional(REQUIRED)
    fun create(
        enkelvoudigInformatieObject: EnkelvoudigInformatieObject,
        zaak: Zaak,
        reason: String
    ): DetachedDocument {
        val detachedDocument = DetachedDocument().apply {
            documentID = enkelvoudigInformatieObject.getIdentificatie()
            documentUUID = enkelvoudigInformatieObject.getUrl().extractUuid()
            creatiedatum = enkelvoudigInformatieObject.getCreatiedatum()
            titel = enkelvoudigInformatieObject.getTitel()
            bestandsnaam = enkelvoudigInformatieObject.getBestandsnaam()
            ontkoppeldOp = ZonedDateTime.now()
            ontkoppeldDoor = loggedInUserInstance.get().id
            zaakID = zaak.getIdentificatie()
            reden = reason
        }
        entityManager.persist(detachedDocument)
        return detachedDocument
    }

    fun getDetachedDocumentResult(listParameters: DetachedDocumentListParameters): DetachedDocumentResult {
        val detachedDocuments = list(listParameters)
        val detachedDocumentsCount = count(listParameters)
        val detachedByFilters = getOntkoppeldDoor(listParameters)
        return DetachedDocumentResult(
            items = detachedDocuments,
            count = detachedDocumentsCount.toLong(),
            detachedByFilter = detachedByFilters
        )
    }

    /**
     * Returns the detached document for the provided enkelvoudiginformatieobject UUID.
     *
     * @param enkelvoudiginformatieobjectUUID the enkelvoudiginformatieobject UUID
     * @return the detached document
     * @throws DetachedDocumentNotFoundException if the detached document could not be found
     */
    fun read(enkelvoudiginformatieobjectUUID: UUID): DetachedDocument =
        find(enkelvoudiginformatieobjectUUID) ?: throw DetachedDocumentNotFoundException(
            "No detached document found for enkelvoudiginformatieobject UUID: '$enkelvoudiginformatieobjectUUID'"
        )

    fun find(id: Long): DetachedDocument? =
        entityManager.find(DetachedDocument::class.java, id)

    fun find(enkelvoudiginformatieobjectUUID: UUID): DetachedDocument? {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(DetachedDocument::class.java)
        val root = query.from(DetachedDocument::class.java)
        query.select(root).where(builder.equal(root.get<Any>("documentUUID"), enkelvoudiginformatieobjectUUID))
        val resultList = entityManager.createQuery(query).getResultList()
        return if (!resultList.isEmpty()) {
            resultList.first()
        } else {
            null
        }
    }

    @Transactional(REQUIRED)
    fun delete(id: Long) { find(id)?.run(entityManager::remove) }

    @Transactional(REQUIRED)
    fun delete(uuid: UUID) {
        find(uuid)?.run(entityManager::remove)
    }

    private fun count(listParameters: DetachedDocumentListParameters): Int {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(Long::class.java)
        val root = query.from(DetachedDocument::class.java)
        query.select(builder.count(root))
        query.where(getWhere(listParameters, root))
        val result = entityManager.createQuery(query).getSingleResult() ?: return 0
        return result.toInt()
    }

    private fun list(listParameters: DetachedDocumentListParameters): List<DetachedDocument> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(DetachedDocument::class.java)
        val root = query.from(DetachedDocument::class.java)
        listParameters.sorting?.let {
            if (it.direction == SorteerRichting.ASCENDING) {
                query.orderBy(builder.asc(root.get<Any>(it.field)))
            } else {
                query.orderBy(builder.desc(root.get<Any>(it.field)))
            }
        }
        query.where(getWhere(listParameters, root))
        val emQuery = entityManager.createQuery(query)
        listParameters.paging?.let {
            emQuery.firstResult = it.getFirstResult()
            emQuery.maxResults = it.maxResults
        }
        return emQuery.getResultList()
    }

    private fun getOntkoppeldDoor(listParameters: DetachedDocumentListParameters): List<String> {
        val builder = entityManager.criteriaBuilder
        val query = builder.createQuery(String::class.java)
        val root = query.from(DetachedDocument::class.java)
        query.select(root.get(DetachedDocument.ONTKOPPELD_DOOR_PROPERTY_NAME)).distinct(true)
        query.where(getWhere(listParameters, root))
        return entityManager.createQuery(query).getResultList()
    }

    private fun getWhere(
        listParameters: DetachedDocumentListParameters,
        root: Root<DetachedDocument>
    ): Predicate {
        val builder = entityManager.criteriaBuilder
        val predicates: MutableList<Predicate> = mutableListOf()
        if (StringUtils.isNotBlank(listParameters.zaakID)) {
            predicates.add(
                builder.like(
                    root.get(ZAAK_ID_PROPERTY_NAME),
                    LIKE.format(listParameters.zaakID)
                )
            )
        }
        listParameters.titel?.let {
            if (it.isNotBlank()) {
                val titel = LIKE.format(it.lowercase(Locale.getDefault()).replace(" ", "%"))
                predicates.add(builder.like(builder.lower(root.get(TITEL_PROPERTY_NAME)), titel))
            }
        }
        listParameters.reden?.let {
            if (it.isNotBlank()) {
                val reden = LIKE.format(it.lowercase(Locale.getDefault()).replace(" ", "%"))
                predicates.add(builder.like(builder.lower(root.get(REDEN_PROPERTY_NAME)), reden))
            }
        }
        listParameters.ontkoppeldDoor?.let {
            if (it.isNotBlank()) {
                predicates.add(
                    builder.equal(
                        root.get<Any>(DetachedDocument.ONTKOPPELD_DOOR_PROPERTY_NAME),
                        it
                    )
                )
            }
        }
        listParameters.creatiedatum?.let {
            addDatumRangePredicates(
                it,
                DetachedDocument.CREATIEDATUM_PROPERTY_NAME,
                predicates,
                root,
                builder
            )
        }
        listParameters.ontkoppeldOp?.let {
            addDatumRangePredicates(
                it,
                DetachedDocument.ONTKOPPELD_OP_PROPERTY_NAME,
                predicates,
                root,
                builder
            )
        }
        @Suppress("SpreadOperator")
        return builder.and(*predicates.toTypedArray<Predicate?>())
    }

    private fun addDatumRangePredicates(
        dateRange: DatumRange,
        veld: String?,
        predicates: MutableList<Predicate>,
        root: Root<DetachedDocument>,
        builder: CriteriaBuilder
    ) {
        dateRange.van?.let {
            predicates.add(
                builder.greaterThanOrEqualTo(
                    root.get(veld),
                    DateTimeUtil.convertToDateTime(it)
                )
            )
        }
        dateRange.tot?.let {
            predicates.add(
                builder.lessThanOrEqualTo(
                    root.get(veld),
                    DateTimeUtil.convertToDateTime(it).plusDays(1)
                        .minusSeconds(1)
                )
            )
        }
    }
}
