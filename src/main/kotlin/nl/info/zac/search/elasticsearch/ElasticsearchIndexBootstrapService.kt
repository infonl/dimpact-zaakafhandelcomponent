/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.elasticsearch

import co.elastic.clients.elasticsearch.ElasticsearchClient
import jakarta.annotation.Resource
import jakarta.enterprise.concurrent.ManagedExecutorService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.Initialized
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import jakarta.inject.Singleton
import nl.info.zac.search.IndexingService
import nl.info.zac.util.NoArgConstructor
import java.time.Duration
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Creates the per-type Elasticsearch indices ([SearchIndex]) on application startup.
 *
 * The bootstrap waits until the cluster is reachable, then creates any index that does not yet exist using
 * its mapping resource. Existing indices are left untouched, so the bootstrap is idempotent across restarts.
 * Indices that were newly created are (re)indexed from the source systems in the background.
 */
@Singleton
@NoArgConstructor
class ElasticsearchIndexBootstrapService @Inject constructor(
    private val elasticsearchClient: ElasticsearchClient,
    private val indexingService: IndexingService
) {
    companion object {
        private val LOG = Logger.getLogger(ElasticsearchIndexBootstrapService::class.java.name)
        private const val WAIT_FOR_ELASTICSEARCH_SECONDS = 1L
    }

    private lateinit var managedExecutor: ManagedExecutorService

    @Resource
    fun setManagedExecutorService(managedExecutor: ManagedExecutorService) {
        this.managedExecutor = managedExecutor
    }

    @Suppress("UNUSED_PARAMETER")
    fun onStartup(@Observes @Initialized(ApplicationScoped::class) event: Any) {
        waitForElasticsearchAvailability()
        SearchIndex.entries.forEach { searchIndex ->
            if (createIndexIfAbsent(searchIndex)) {
                startReindexing(searchIndex)
            }
        }
    }

    /**
     * @return `true` when the index was newly created, `false` when it already existed.
     */
    private fun createIndexIfAbsent(searchIndex: SearchIndex): Boolean {
        if (elasticsearchClient.indices().exists { it.index(searchIndex.indexName) }.value()) {
            LOG.info("Elasticsearch index '${searchIndex.indexName}' already exists. Leaving it intact.")
            return false
        }
        LOG.info("Creating Elasticsearch index '${searchIndex.indexName}'")
        javaClass.getResourceAsStream(searchIndex.mappingResource).use { mappingStream ->
            requireNotNull(mappingStream) { "Index mapping resource '${searchIndex.mappingResource}' not found" }
            elasticsearchClient.indices().create { builder ->
                builder.index(searchIndex.indexName).withJson(mappingStream)
            }
        }
        return true
    }

    private fun waitForElasticsearchAvailability() {
        while (true) {
            try {
                if (elasticsearchClient.ping().value()) {
                    LOG.info("Elasticsearch cluster is available")
                    return
                }
            } catch (@Suppress("TooGenericExceptionCaught") exception: Exception) {
                LOG.info("Elasticsearch is not available yet. Exception: ${exception.message}")
            }
            LOG.info("Waiting $WAIT_FOR_ELASTICSEARCH_SECONDS second(s) for Elasticsearch to become available...")
            try {
                Thread.sleep(Duration.ofSeconds(WAIT_FOR_ELASTICSEARCH_SECONDS).toMillis())
            } catch (exception: InterruptedException) {
                LOG.log(Level.WARNING, "Interrupted while waiting for Elasticsearch. Re-interrupting thread.", exception)
                Thread.currentThread().interrupt()
                return
            }
        }
    }

    private fun startReindexing(searchIndex: SearchIndex) {
        LOG.info("Scheduling reindex of newly created index '${searchIndex.indexName}'")
        managedExecutor.submit { indexingService.reindex(searchIndex.objectType) }
    }
}
