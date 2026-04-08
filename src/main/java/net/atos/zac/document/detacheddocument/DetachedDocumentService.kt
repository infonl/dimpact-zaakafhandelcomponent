/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.document.detacheddocument

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import jakarta.transaction.Transactional
import net.atos.client.zgw.shared.util.DateTimeUtil
import net.atos.zac.document.detacheddocument.model.DetachedDocument
import net.atos.zac.document.detacheddocument.model.DetachedDocumentListParameters
import net.atos.zac.document.detacheddocument.model.DetachedDocumentResult
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.zac.app.informatieobjecten.exception.DetachedDocumentNotFoundException
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.search.model.DatumRange
import nl.info.zac.shared.model.SorteerRichting
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.apache.commons.lang3.StringUtils
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID

@ApplicationScoped
@Transactional
@NoArgConstructor
@AllOpen
@Suppress("TooManyFunctions")
class DetachedDocumentService @Inject constructor(
    private val entityManager: EntityManager,
    private val loggedInUserInstance: Instance<LoggedInUser>
) {
    companion object {
        private const val LIKE = "%%%s%%"
    }

    fun create(
        informatieobject: EnkelvoudigInformatieObject,
        zaak: Zaak,
        reden: String?
    ): DetachedDocument {
        val detachedDocument = DetachedDocument().apply {
            documentID = informatieobject.getIdentificatie()
            documentUUID = informatieobject.getUrl().extractUuid()
            creatiedatum = informatieobject.getCreatiedatum()
            titel = informatieobject.getTitel()
            bestandsnaam = informatieobject.getBestandsnaam()
            ontkoppeldOp = ZonedDateTime.now()
            ontkoppeldDoor = loggedInUserInstance.get()!!.id
            zaakID = zaak.getIdentificatie()
            this.reden = reden
        }
        entityManager.persist(detachedDocument)
        return detachedDocument
    }

    fun getResultaat(listParameters: DetachedDocumentListParameters) = DetachedDocumentResult(
        items = list(listParameters),
        count = count(listParameters).toLong(),
        ontkoppeldDoorFilter = getOntkoppeldDoor(listParameters)
    )

    /**
     * Returns the detached document for the provided enkelvoudiginformatieobject UUID.
     *
     * @param enkelvoudiginformatieobjectUUID the enkelvoudiginformatieobject UUID
     * @return the detached document
     * @throws DetachedDocumentNotFoundException if the detached document could not be found
     */
    fun read(enkelvoudiginformatieobjectUUID: UUID): DetachedDocument =
        find(enkelvoudiginformatieobjectUUID) ?: throw DetachedDocumentNotFoundException(
            "Detached document with enkelvoudiginformatieobject UUID '$enkelvoudiginformatieobjectUUID' not found"
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
        if (listParameters.sorting != null) {
            if (listParameters.sorting!!.direction == SorteerRichting.ASCENDING) {
                query.orderBy(builder.asc(root.get<Any>(listParameters.sorting!!.field)))
            } else {
                query.orderBy(builder.desc(root.get<Any>(listParameters.sorting!!.field)))
            }
        }
        query.where(getWhere(listParameters, root))
        val emQuery = entityManager.createQuery(query)
        if (listParameters.paging != null) {
            emQuery.firstResult = listParameters.paging!!.getFirstResult()
            emQuery.maxResults = listParameters.paging!!.maxResults
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

    fun delete(id: Long) { find(id)?.run(entityManager::remove) }

    fun delete(uuid: UUID) {
        find(uuid)?.run(entityManager::remove)
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
                    root.get<String?>(DetachedDocument.ZAAK_ID_PROPERTY_NAME),
                    LIKE.format(listParameters.zaakID)
                )
            )
        }
        if (StringUtils.isNotBlank(listParameters.titel)) {
            val titel: String = LIKE.format(listParameters.titel!!.lowercase(Locale.getDefault()).replace(" ", "%"))
            predicates.add(builder.like(builder.lower(root.get<String?>(DetachedDocument.TITEL_PROPERTY_NAME)), titel))
        }
        if (StringUtils.isNotBlank(listParameters.reden)) {
            val reden: String = LIKE.format(listParameters.reden!!.lowercase(Locale.getDefault()).replace(" ", "%"))
            predicates.add(builder.like(builder.lower(root.get<String?>(DetachedDocument.REDEN_PROPERTY_NAME)), reden))
        }

        if (StringUtils.isNotBlank(listParameters.ontkoppeldDoor)) {
            predicates.add(
                builder.equal(
                    root.get<Any?>(DetachedDocument.ONTKOPPELD_DOOR_PROPERTY_NAME),
                    listParameters.ontkoppeldDoor
                )
            )
        }
        addDatumRangePredicates(
            listParameters.creatiedatum,
            DetachedDocument.CREATIEDATUM_PROPERTY_NAME,
            predicates,
            root,
            builder
        )
        addDatumRangePredicates(
            listParameters.ontkoppeldOp,
            DetachedDocument.ONTKOPPELD_OP_PROPERTY_NAME,
            predicates,
            root,
            builder
        )

        return builder.and(*predicates.toTypedArray<Predicate?>())
    }

    private fun addDatumRangePredicates(
        dateRange: DatumRange?,
        veld: String?,
        predicates: MutableList<Predicate>,
        root: Root<DetachedDocument>,
        builder: CriteriaBuilder
    ) {
        dateRange?.let { datumRange ->
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
}
