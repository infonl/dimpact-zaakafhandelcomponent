/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.solr

import jakarta.annotation.Resource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.Initialized
import jakarta.enterprise.concurrent.ManagedExecutorService
import jakarta.enterprise.event.Observes
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import nl.info.zac.search.IndexingService
import nl.info.zac.search.IndexingService.Companion.SOLR_CORE
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.solr.FieldType.STRING
import nl.info.zac.solr.exception.SolrDeploymentException
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.apache.commons.lang3.StringUtils
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.SolrServerException
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.client.solrj.request.SolrPing
import org.apache.solr.client.solrj.request.schema.SchemaRequest
import org.apache.solr.common.SolrException
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.io.IOException
import java.time.Duration
import java.util.logging.Logger

@Singleton
@NoArgConstructor
@AllOpen
class SolrDeployerService @Inject constructor(
    @ConfigProperty(name = "SOLR_URL") private val solrUrl: String,
    private val indexingService: IndexingService
) {
    companion object {
        private val LOG = Logger.getLogger(SolrDeployerService::class.java.name)
        private const val VERSION_FIELD_PREFIX = "schema_version_"
        private const val SOLR_STATUS_OK = 0
        private const val WAIT_FOR_SOLR_SECONDS = 1L
    }

    private lateinit var managedExecutor: ManagedExecutorService
    private lateinit var solrClient: SolrClient
    private lateinit var schemaUpdates: List<SolrSchemaUpdate>

    @Inject
    fun setSchemaUpdates(schemaUpdates: Instance<SolrSchemaUpdate>) {
        this.schemaUpdates = schemaUpdates.sortedBy { it.versie }
    }

    @Resource
    fun setManagedExecutorService(managedExecutor: ManagedExecutorService) {
        this.managedExecutor = managedExecutor
    }

    fun onStartup(@Observes @Initialized(ApplicationScoped::class) @Suppress("UNUSED_PARAMETER") event: Any) {
        solrClient = Http2SolrClient.Builder("$solrUrl/solr/$SOLR_CORE").build()
        waitForSolrAvailability()
        try {
            val currentVersion = getCurrentVersion()
            LOG.info { "Current version of Solr core '$SOLR_CORE': $currentVersion" }
            if (currentVersion == schemaUpdates.last().versie) {
                LOG.info { "Solr core '$SOLR_CORE' is up to date. No Solr schema migration needed." }
            } else {
                schemaUpdates.drop(currentVersion).forEach(::apply)
                schemaUpdates.drop(currentVersion)
                    .flatMap { it.teHerindexerenZoekObjectTypes }
                    .toSet()
                    .takeIf { it.isNotEmpty() }
                    ?.let(::startReindexing)
            }
        } catch (solrServerException: SolrServerException) {
            throw SolrDeploymentException("Failed to deploy Solr schema for core '$SOLR_CORE'", solrServerException)
        } catch (ioException: IOException) {
            throw SolrDeploymentException("Failed to deploy Solr schema for core '$SOLR_CORE'", ioException)
        }
    }

    private fun waitForSolrAvailability() {
        while (true) {
            try {
                if (SolrPing().setActionPing().process(solrClient).status == SOLR_STATUS_OK) {
                    return
                }
            } catch (solrServerException: SolrServerException) {
                LOG.info { "Solr core is not available yet. Exception: ${solrServerException.message}" }
            } catch (ioException: IOException) {
                LOG.info { "Solr core is not available yet. Exception: ${ioException.message}" }
            } catch (solrException: SolrException) {
                LOG.info { "Solr core is not available yet. Exception: ${solrException.message}" }
            }
            LOG.info { "Waiting for $WAIT_FOR_SOLR_SECONDS seconds for Solr core '$SOLR_CORE' to become available..." }
            try {
                Thread.sleep(Duration.ofSeconds(WAIT_FOR_SOLR_SECONDS).toMillis())
            } catch (interruptedException: InterruptedException) {
                Thread.currentThread().interrupt()
                throw SolrDeploymentException(
                    "Thread was interrupted while waiting for Solr core '$SOLR_CORE' to become available",
                    interruptedException
                )
            }
        }
    }

    private fun getCurrentVersion(): Int =
        SchemaRequest.Fields().process(solrClient).fields
            .map { it[NAME].toString() }.firstOrNull { it.startsWith(VERSION_FIELD_PREFIX) }
            ?.let { StringUtils.substringAfter(it, VERSION_FIELD_PREFIX).toInt() }
            ?: 0

    private fun apply(schemaUpdate: SolrSchemaUpdate) {
        LOG.info { "Updating Solr core '$SOLR_CORE' to version: ${schemaUpdate.versie}" }
        try {
            val schemaRequestUpdates = schemaUpdate.schemaUpdates + updateVersionField(schemaUpdate.versie)
            SchemaRequest.MultiUpdate(schemaRequestUpdates).process(solrClient)
        } catch (solrServerException: SolrServerException) {
            throw SolrDeploymentException(
                "Failed to update Solr core '$SOLR_CORE' to version: ${schemaUpdate.versie}",
                solrServerException
            )
        } catch (ioException: IOException) {
            throw SolrDeploymentException(
                "Failed to update Solr core '$SOLR_CORE' to version: ${schemaUpdate.versie}",
                ioException
            )
        }
    }

    private fun updateVersionField(version: Int): List<SchemaRequest.Update> = buildList {
        if (version > 1) {
            add(deleteField(VERSION_FIELD_PREFIX + (version - 1)))
        }
        add(addField(name = VERSION_FIELD_PREFIX + version, type = STRING, indexed = false, stored = false))
    }

    private fun startReindexing(types: Set<ZoekObjectType>) {
        managedExecutor.submit { indexingService.reindexAll(types) }
    }
}
