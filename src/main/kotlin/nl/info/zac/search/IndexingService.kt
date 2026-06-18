/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch.core.BulkRequest
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.atos.client.zgw.shared.model.Results
import net.atos.client.zgw.zrc.model.ZaakListParameters
import net.atos.zac.flowable.task.FlowableTaskService
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.EnkelvoudigInformatieobjectListParameters
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.app.task.model.TaakSortering
import nl.info.zac.authentication.LoggedInUserProvider.Companion.systemUser
import nl.info.zac.search.converter.AbstractZoekObjectConverter
import nl.info.zac.search.elasticsearch.ElasticsearchClientProducer
import nl.info.zac.search.elasticsearch.SearchIndex
import nl.info.zac.search.model.zoekobject.ZaakZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObject
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.shared.model.SorteerRichting
import nl.info.zac.util.AllOpen
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger

@Singleton
@AllOpen
@Suppress("TooManyFunctions")
class IndexingService @Inject constructor(
    private val converterInstances: Instance<AbstractZoekObjectConverter<out ZoekObject>>,
    private val zrcClientService: ZrcClientService,
    private val drcClientService: DrcClientService,
    private val flowableTaskService: FlowableTaskService,
    private val elasticsearchClient: ElasticsearchClient
) {
    companion object {
        const val INDEXING_ERROR_MESSAGE = "Error occurred during Elasticsearch indexing"

        private const val TAKEN_MAX_RESULTS = 50

        private val LOG = Logger.getLogger(IndexingService::class.java.name)
        private val reindexingViewfinder = ConcurrentHashMap.newKeySet<ZoekObjectType>()
        private val objectMapper: ObjectMapper = ElasticsearchClientProducer.createObjectMapper()
    }

    /**
     * Adds objectId to the Elasticsearch index for its type and optionally refreshes the index so that the
     * change becomes searchable immediately. Beware that an index refresh is a relatively expensive operation.
     *
     * @param objectId      the object id to be indexed
     * @param objectType    the object type
     * @param performCommit whether to refresh the index immediately
     */
    fun indexeerDirect(objectId: String, objectType: ZoekObjectType, performCommit: Boolean) =
        addToIndex(
            getConverter(objectType).let { converter ->
                listOf(
                    continueOnExceptions(objectType) { converter.convert(objectId) }
                )
            },
            performCommit
        )

    /**
     * Adds a list of objectIds to the Elasticsearch index for their type and optionally refreshes the index
     * so that the changes become searchable immediately.
     *
     * @param objectIds     the list of object ids to be indexed
     * @param objectType    the object type
     * @param performCommit whether to refresh the index immediately
     */
    fun indexeerDirect(objectIds: List<String>, objectType: ZoekObjectType, performCommit: Boolean) =
        addToIndex(
            getConverter(objectType).let { converter ->
                objectIds.map { continueOnExceptions(objectType) { converter.convert(it) } }
            },
            performCommit
        )

    fun reindex(objectType: ZoekObjectType) {
        if (reindexingViewfinder.contains(objectType)) {
            LOG.warning("[$objectType] Reindexing not started, still in progress")
            return
        }
        reindexingViewfinder.add(objectType)
        try {
            systemUser.set(true)
            LOG.info("[$objectType] Reindexing started")
            removeEntitiesFromIndex(objectType)
            when (objectType) {
                ZoekObjectType.ZAAK -> reindexAllZaken()
                ZoekObjectType.DOCUMENT -> reindexAllInformatieobjecten()
                ZoekObjectType.TAAK -> reindexAllTaken()
            }
            LOG.info("[$objectType] Reindexing finished")
        } finally {
            reindexingViewfinder.remove(objectType)
            systemUser.remove()
        }
    }

    fun addOrUpdateZaak(zaakUUID: UUID, inclusiefTaken: Boolean) {
        indexeerDirect(zaakUUID.toString(), ZoekObjectType.ZAAK, false)
        if (inclusiefTaken) {
            flowableTaskService.listOpenTasksForZaak(zaakUUID)
                .map { it.id }
                .forEach(this::addOrUpdateTaak)
        }
    }

    fun addOrUpdateInformatieobject(informatieobjectUUID: UUID) =
        indexeerDirect(informatieobjectUUID.toString(), ZoekObjectType.DOCUMENT, false)

    fun addOrUpdateInformatieobjectByZaakinformatieobject(zaakinformatieobjectUUID: UUID) =
        addOrUpdateInformatieobject(
            zrcClientService.readZaakinformatieobject(zaakinformatieobjectUUID).informatieobject.extractUuid()
        )

    fun addOrUpdateTaak(taskID: String) = indexeerDirect(taskID, ZoekObjectType.TAAK, false)

    fun removeZaak(zaakUUID: UUID) = removeFromIndex(ZoekObjectType.ZAAK, zaakUUID.toString())

    fun removeInformatieobject(informatieobjectUUID: UUID) =
        removeFromIndex(ZoekObjectType.DOCUMENT, informatieobjectUUID.toString())

    fun removeTaak(taskID: String) = removeFromIndex(ZoekObjectType.TAAK, taskID)

    fun commit() {
        runTranslatingToIndexingException {
            elasticsearchClient.indices().refresh { it.index(SearchIndex.ALL_INDEX_NAMES) }
        }
    }

    private fun getConverter(objectType: ZoekObjectType): AbstractZoekObjectConverter<out ZoekObject> =
        converterInstances
            .firstOrNull { it.supports(objectType) }
            ?: throw IndexingException("[$objectType] No converter found")

    private fun addToIndex(zoekObjecten: List<ZoekObject?>, performCommit: Boolean) {
        val beansToBeAdded = zoekObjecten.filterNotNull()
        if (beansToBeAdded.isEmpty()) {
            return
        }
        runTranslatingToIndexingException {
            val bulkRequest = BulkRequest.Builder()
            beansToBeAdded.forEach { zoekObject ->
                val indexName = SearchIndex.indexNameForType(zoekObject.getType())
                val document = toDocument(zoekObject)
                bulkRequest.operations { operation ->
                    operation.index { indexOperation ->
                        indexOperation.index(indexName).id(zoekObject.getObjectId()).document(document)
                    }
                }
            }
            elasticsearchClient.bulk(bulkRequest.build())
            if (performCommit) {
                commit()
            }
        }
    }

    /**
     * Serializes a [ZoekObject] to a document map, flattening the dynamic `zaak_betrokkene_<rol>` fields of a
     * [ZaakZoekObject] to top-level fields (so the index dynamic template can map them).
     */
    private fun toDocument(zoekObject: ZoekObject): Map<String, Any?> {
        @Suppress("UNCHECKED_CAST")
        val document = objectMapper.convertValue(zoekObject, MutableMap::class.java) as MutableMap<String, Any?>
        if (zoekObject is ZaakZoekObject) {
            zoekObject.betrokkenen?.forEach { (field, values) -> document[field] = values }
        }
        return document
    }

    private fun removeFromIndex(objectType: ZoekObjectType, id: String) {
        runTranslatingToIndexingException {
            elasticsearchClient.delete { it.index(SearchIndex.indexNameForType(objectType)).id(id) }
        }
    }

    private fun removeEntitiesFromIndex(objectType: ZoekObjectType) {
        continueOnExceptions(objectType) {
            elasticsearchClient.deleteByQuery { request ->
                request
                    .index(SearchIndex.indexNameForType(objectType))
                    .query { query -> query.matchAll { it } }
                    .refresh(true)
            }
        }
    }

    private fun reindexAllZaken() {
        val numberOfZaken = continueOnExceptions(ZoekObjectType.ZAAK) {
            zrcClientService.listZakenUuids(
                ZaakListParameters().apply {
                    ordering = "-identificatie"
                    page = ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS
                }
            ).count()
        }
        if (numberOfZaken == null) {
            LOG.warning("[${ZoekObjectType.ZAAK}] Cannot find zaken count! Aborting reindexing")
            return
        }

        val numberOfPages: Int = numberOfZaken / Results.NUM_ITEMS_PER_PAGE.toInt() +
            ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS

        for (pageNumber in ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS..numberOfPages) {
            continueOnExceptions(ZoekObjectType.ZAAK) { reindexZakenPage(pageNumber, numberOfZaken) }
        }
    }

    private fun reindexZakenPage(pageNumber: Int, totalCount: Int) {
        val zaakResults = zrcClientService.listZakenUuids(
            ZaakListParameters().apply {
                ordering = "-identificatie"
                page = pageNumber
            }
        )
        val ids = zaakResults.results().map { it.uuid.toString() }
        indexeerDirect(
            objectIds = ids,
            objectType = ZoekObjectType.ZAAK,
            performCommit = false
        )
        val progress = (pageNumber - ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS) * Results.NUM_ITEMS_PER_PAGE + ids.size
        LOG.info("[${ZoekObjectType.ZAAK}] Reindexed: $progress / $totalCount ")
    }

    private fun reindexAllInformatieobjecten() {
        val numberOfInformatieobjecten = continueOnExceptions(ZoekObjectType.DOCUMENT) {
            drcClientService.listEnkelvoudigInformatieObjecten(
                EnkelvoudigInformatieobjectListParameters().apply {
                    page = ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS
                }
            ).count()
        }
        if (numberOfInformatieobjecten == null) {
            LOG.warning("[${ZoekObjectType.DOCUMENT}] Cannot find information objects count! Aborting reindexing")
            return
        }

        val numberOfPages: Int = numberOfInformatieobjecten / Results.NUM_ITEMS_PER_PAGE.toInt() +
            ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS

        for (pageNumber in ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS..numberOfPages) {
            continueOnExceptions(ZoekObjectType.DOCUMENT) {
                reindexInformatieobjectenPage(pageNumber, numberOfInformatieobjecten)
            }
        }
    }

    private fun reindexInformatieobjectenPage(pageNumber: Int, totalCount: Int) {
        val informationObjectsResults = drcClientService.listEnkelvoudigInformatieObjecten(
            EnkelvoudigInformatieobjectListParameters().apply { page = ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS }
        )
        val ids = informationObjectsResults.results().map { it.url.extractUuid().toString() }
        indexeerDirect(
            objectIds = ids,
            objectType = ZoekObjectType.DOCUMENT,
            performCommit = false
        )
        val progress = (pageNumber - ZgwApiService.FIRST_PAGE_NUMBER_ZGW_APIS) * Results.NUM_ITEMS_PER_PAGE + ids.size
        LOG.info("[${ZoekObjectType.DOCUMENT}] Reindexed: $progress / $totalCount")
    }

    private fun reindexAllTaken() {
        val numberOfTasks = continueOnExceptions(ZoekObjectType.TAAK) { flowableTaskService.countOpenTasks() }
        if (numberOfTasks == null) {
            LOG.warning("[${ZoekObjectType.TAAK}] Cannot find tasks count. Aborting reindexing")
            return
        }
        val numberOfPages: Int = numberOfTasks.toInt() / TAKEN_MAX_RESULTS

        for (pageNumber in 0..numberOfPages) {
            continueOnExceptions(ZoekObjectType.TAAK) { reindexTakenPage(pageNumber, numberOfTasks.toInt()) }
        }
    }

    private fun reindexTakenPage(pageNumber: Int, totalCount: Int): Boolean {
        val firstResult = pageNumber * TAKEN_MAX_RESULTS
        val tasks = flowableTaskService.listOpenTasks(
            TaakSortering.CREATIEDATUM,
            SorteerRichting.DESCENDING,
            firstResult,
            TAKEN_MAX_RESULTS
        )
        if (tasks.isEmpty()) {
            return false
        }
        indexeerDirect(
            objectIds = tasks.map { it.id },
            objectType = ZoekObjectType.TAAK,
            performCommit = false
        )
        val progress = firstResult + tasks.size
        LOG.info("[${ZoekObjectType.TAAK}] Reindexed: $progress / $totalCount")
        return tasks.size == TAKEN_MAX_RESULTS
    }

    @Suppress("TooGenericExceptionCaught")
    private fun <T> runTranslatingToIndexingException(fn: () -> T): T {
        try {
            return fn()
        } catch (exception: Exception) {
            throw IndexingException(INDEXING_ERROR_MESSAGE, exception)
        }
    }

    private fun <T> continueOnExceptions(objectType: ZoekObjectType, fn: () -> T): T? =
        try {
            runTranslatingToIndexingException { fn() }
        } catch (indexingException: IndexingException) {
            LOG.log(Level.WARNING, "[$objectType] Error during indexing", indexingException)
            null
        }
}
