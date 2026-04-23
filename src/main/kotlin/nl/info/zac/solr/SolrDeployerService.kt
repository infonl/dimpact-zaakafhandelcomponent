/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.solr

import jakarta.annotation.Resource
import jakarta.enterprise.concurrent.ManagedExecutorService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.Initialized
import jakarta.enterprise.event.Observes
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.atos.zac.solr.FieldType.STRING
import net.atos.zac.solr.SolrSchemaUpdate
import net.atos.zac.solr.SolrSchemaUpdateHelper.NAME
import net.atos.zac.solr.SolrSchemaUpdateHelper.addField
import net.atos.zac.solr.SolrSchemaUpdateHelper.deleteField
import nl.info.zac.search.IndexingService
import nl.info.zac.search.IndexingService.Companion.SOLR_CORE
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.util.AllOpen
import org.apache.commons.lang3.StringUtils
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.SolrServerException
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.client.solrj.request.CollectionAdminRequest
import org.apache.solr.client.solrj.request.SolrPing
import org.apache.solr.client.solrj.request.schema.SchemaRequest
import org.apache.solr.common.SolrException
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.io.IOException
import java.time.Duration
import java.util.logging.Level
import java.util.logging.Logger

@Singleton
@AllOpen
@Suppress("TooManyFunctions")
class SolrDeployerService @Inject constructor(
    @ConfigProperty(name = "SOLR_URL") private val solrUrl: String,
    @ConfigProperty(name = "SOLR_COLLECTION_NUM_SHARDS", defaultValue = "1") private val numShards: Int,
    @ConfigProperty(
        name = "SOLR_COLLECTION_REPLICATION_FACTOR",
        defaultValue = "1"
    ) private val replicationFactor: Int,
    private val indexingService: IndexingService
) {
    companion object {
        const val SOLR_COLLECTION_A = "zac_a"
        const val SOLR_COLLECTION_B = "zac_b"

        private const val VERSION_FIELD_PREFIX = "schema_version_"
        private const val SOLR_STATUS_OK = 0
        private val WAIT_FOR_SOLR_DURATION = Duration.ofSeconds(1)
        private val LOG = Logger.getLogger(SolrDeployerService::class.java.name)
    }

    private lateinit var managedExecutor: ManagedExecutorService
    private lateinit var schemaUpdates: List<SolrSchemaUpdate>

    @Resource
    fun setManagedExecutorService(managedExecutor: ManagedExecutorService) {
        this.managedExecutor = managedExecutor
    }

    @Inject
    fun setSchemaUpdates(schemaUpdates: Instance<SolrSchemaUpdate>) {
        this.schemaUpdates = schemaUpdates.stream()
            .sorted(compareBy { it.versie })
            .toList()
    }

    fun onStartup(@Observes @Initialized(ApplicationScoped::class) event: Any) {
        val adminClient = Http2SolrClient.Builder("$solrUrl/solr").build()
        val isSolrCloud = initializeSolrCloud(adminClient)

        val solrClient = Http2SolrClient.Builder("$solrUrl/solr/$SOLR_CORE").build()
        waitForSolrAvailability(solrClient)

        val currentVersion = getCurrentVersion(solrClient)
        LOG.info("Current version of Solr core '$SOLR_CORE': $currentVersion")

        if (currentVersion == schemaUpdates.last().versie) {
            LOG.info("Solr core '$SOLR_CORE' is up to date. No Solr schema migration needed.")
            return
        }

        val pendingUpdates = schemaUpdates.drop(currentVersion)
        val typesToReindex = pendingUpdates.flatMapTo(mutableSetOf()) { it.teHerindexerenZoekObjectTypes }

        if (!isSolrCloud || typesToReindex.isEmpty()) {
            // Fast path: schema-only changes or legacy (non-SolrCloud) mode
            LOG.info("Applying ${pendingUpdates.size} schema update(s) to '$SOLR_CORE'")
            pendingUpdates.forEach { apply(solrClient, it) }
            typesToReindex.forEach { startReindexing(it) }
            return
        }

        // Blue-green reindex: keeps old collection serving reads while new one is populated
        val activeCollection = getActiveCollection(adminClient)
        val inactiveCollection = getInactiveCollectionName(activeCollection)

        if (collectionExists(adminClient, inactiveCollection)) {
            LOG.warning("Deleting stale inactive collection '$inactiveCollection' from a previous failed migration")
            deleteCollection(adminClient, inactiveCollection)
        }

        LOG.info("Creating inactive collection '$inactiveCollection' for blue-green reindex")
        createCollection(adminClient, inactiveCollection)
        applySchemaToCollection(inactiveCollection)

        val reindexFutures = typesToReindex.map { type ->
            managedExecutor.submit(Runnable { indexingService.reindex(type, inactiveCollection) })
        }

        // Submit a follow-up task that waits for all reindexing to complete, then switches alias
        managedExecutor.submit(Runnable {
            reindexFutures.forEach { future ->
                runCatching { future.get() }
                    .onFailure { exception ->
                        LOG.log(Level.SEVERE, "Reindexing task failed during blue-green migration", exception)
                    }
            }
            runCatching {
                switchAlias(adminClient, inactiveCollection)
                deleteCollection(adminClient, activeCollection)
                LOG.info("Alias '$SOLR_CORE' switched to '$inactiveCollection', deleted old collection '$activeCollection'")
            }.onFailure { exception ->
                LOG.log(Level.SEVERE, "Failed to switch Solr alias after reindexing", exception)
            }
        })
    }

    /**
     * Applies the current schema (all versions from V0 to latest) to the specified collection.
     * Used when creating a new collection for blue-green reindexing, and exposed as an endpoint
     * for the manual reindex script.
     */
    fun applySchemaToCollection(collectionName: String) {
        LOG.info("Applying schema to collection '$collectionName'")
        val client = Http2SolrClient.Builder("$solrUrl/solr/$collectionName").build()
        val currentVersion = getCurrentVersion(client)
        schemaUpdates.drop(currentVersion).forEach { apply(client, it) }
    }

    /**
     * Tries to initialize SolrCloud mode by checking the Collections API.
     * If SolrCloud is available, ensures the alias and base collection exist.
     * Returns true for SolrCloud mode, false for standalone/legacy mode.
     */
    private fun initializeSolrCloud(adminClient: SolrClient): Boolean {
        while (true) {
            try {
                CollectionAdminRequest.List().process(adminClient)
                // Collections API available → SolrCloud mode confirmed
                ensureAliasExists(adminClient)
                return true
            } catch (exception: SolrException) {
                if (exception.code() == SolrException.ErrorCode.BAD_REQUEST.code) {
                    // Solr is up but Collections API is not available (standalone mode)
                    LOG.warning(
                        "Solr Collections API returned HTTP 400: ${exception.message}. " +
                            "Falling back to legacy standalone mode (no blue-green alias switching)."
                    )
                    return false
                }
                LOG.info("Waiting for Solr Collections API: ${exception.message}")
            } catch (exception: SolrServerException) {
                LOG.info("Waiting for Solr Collections API: ${exception.message}")
            } catch (exception: IOException) {
                LOG.info("Waiting for Solr Collections API: ${exception.message}")
            }
            Thread.sleep(WAIT_FOR_SOLR_DURATION.toMillis())
        }
    }

    private fun ensureAliasExists(adminClient: SolrClient) {
        if (aliasExists(adminClient, SOLR_CORE)) {
            LOG.info("Solr alias '$SOLR_CORE' already exists")
            return
        }
        if (!collectionExists(adminClient, SOLR_COLLECTION_A)) {
            LOG.info("Creating initial Solr collection '$SOLR_COLLECTION_A'")
            createCollection(adminClient, SOLR_COLLECTION_A)
        }
        LOG.info("Creating Solr alias '$SOLR_CORE' pointing to '$SOLR_COLLECTION_A'")
        CollectionAdminRequest.createAlias(SOLR_CORE, SOLR_COLLECTION_A).process(adminClient)
    }

    private fun waitForSolrAvailability(solrClient: SolrClient) {
        while (true) {
            try {
                if (SolrPing().setActionPing().process(solrClient).status == SOLR_STATUS_OK) {
                    return
                }
            } catch (exception: SolrServerException) {
                LOG.info("Solr core not available yet: ${exception.message}")
            } catch (exception: IOException) {
                LOG.info("Solr core not available yet: ${exception.message}")
            } catch (exception: SolrException) {
                LOG.info("Solr core not available yet: ${exception.message}")
            }
            LOG.info("Waiting for Solr core '$SOLR_CORE' to become available...")
            Thread.sleep(WAIT_FOR_SOLR_DURATION.toMillis())
        }
    }

    private fun getCurrentVersion(client: SolrClient): Int =
        SchemaRequest.Fields().process(client).fields
            .map { field -> field[NAME].toString() }
            .filter { fieldName -> fieldName.startsWith(VERSION_FIELD_PREFIX) }
            .firstOrNull()
            ?.let { versionFieldName ->
                StringUtils.substringAfter(versionFieldName, VERSION_FIELD_PREFIX).toIntOrNull()
            }
            ?: 0

    private fun apply(client: SolrClient, schemaUpdate: SolrSchemaUpdate) {
        LOG.info("Updating Solr schema to version: ${schemaUpdate.versie}")
        val updates = buildList {
            addAll(schemaUpdate.schemaUpdates)
            addAll(updateVersionField(schemaUpdate.versie))
        }
        SchemaRequest.MultiUpdate(updates).process(client)
    }

    private fun updateVersionField(version: Int): List<SchemaRequest.Update> = buildList {
        if (version > 1) {
            add(deleteField(VERSION_FIELD_PREFIX + (version - 1)))
        }
        add(addField(VERSION_FIELD_PREFIX + version, STRING, false, false))
    }

    private fun startReindexing(type: ZoekObjectType) {
        managedExecutor.submit(Runnable { indexingService.reindex(type) })
    }

    @Suppress("UNCHECKED_CAST")
    private fun aliasExists(adminClient: SolrClient, aliasName: String): Boolean {
        val aliases = CollectionAdminRequest.ListAliases().process(adminClient).response.get("aliases")
            as? org.apache.solr.common.util.NamedList<*>
        return aliases?.get(aliasName) != null
    }

    @Suppress("UNCHECKED_CAST")
    private fun collectionExists(adminClient: SolrClient, name: String): Boolean {
        val collections = CollectionAdminRequest.List().process(adminClient).response.get("collections")
            as? List<String>
        return collections?.contains(name) == true
    }

    private fun getActiveCollection(adminClient: SolrClient): String {
        @Suppress("UNCHECKED_CAST")
        val aliases = CollectionAdminRequest.ListAliases().process(adminClient).response.get("aliases")
            as? org.apache.solr.common.util.NamedList<*>
        return aliases?.get(SOLR_CORE)?.toString() ?: SOLR_COLLECTION_A
    }

    private fun getInactiveCollectionName(activeCollection: String) =
        if (activeCollection == SOLR_COLLECTION_A) SOLR_COLLECTION_B else SOLR_COLLECTION_A

    private fun createCollection(adminClient: SolrClient, name: String) {
        LOG.info("Creating Solr collection '$name' (shards=$numShards, replication=$replicationFactor)")
        CollectionAdminRequest.createCollection(name, "_default", numShards, replicationFactor)
            .process(adminClient)
    }

    private fun switchAlias(adminClient: SolrClient, newCollection: String) {
        LOG.info("Switching Solr alias '$SOLR_CORE' to collection '$newCollection'")
        CollectionAdminRequest.createAlias(SOLR_CORE, newCollection).process(adminClient)
    }

    private fun deleteCollection(adminClient: SolrClient, name: String) {
        LOG.info("Deleting Solr collection '$name'")
        CollectionAdminRequest.deleteCollection(name).process(adminClient)
    }
}
