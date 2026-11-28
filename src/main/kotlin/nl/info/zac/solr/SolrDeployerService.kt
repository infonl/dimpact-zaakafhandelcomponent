/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.solr

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.Initialized
import jakarta.enterprise.event.Observes
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import nl.info.zac.log.ZacLogger
import nl.info.zac.search.IndexingService
import nl.info.zac.search.IndexingService.Companion.SOLR_CORE
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.solr.FieldType.STRING
import nl.info.zac.solr.exception.SolrDeploymentException
import nl.info.zac.solr.log.SolrCoreNotAvailable
import nl.info.zac.solr.log.SolrCoreUpdateStarted
import nl.info.zac.solr.log.SolrCoreVersionChecked
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.SolrServerException
import org.apache.solr.client.solrj.request.SolrPing
import org.apache.solr.client.solrj.request.schema.SchemaRequest
import org.apache.solr.common.SolrException
import java.io.IOException
import java.time.Duration

@Singleton
@NoArgConstructor
@AllOpen
class SolrDeployerService @Inject constructor(
    private val solrClientFactory: SolrClientFactory,
    private val indexingService: IndexingService
) {
    companion object {
        private val LOG = ZacLogger.getLogger(SolrDeployerService::class)
        private const val VERSION_FIELD_PREFIX = "schema_version_"
        private const val SOLR_STATUS_OK = 0
        private const val WAIT_FOR_SOLR_SECONDS = 1L
    }

    private lateinit var solrClient: SolrClient
    private lateinit var schemaUpdates: List<SolrSchemaUpdate>

    @Inject
    fun setSchemaUpdates(schemaUpdates: Instance<SolrSchemaUpdate>) {
        this.schemaUpdates = schemaUpdates.sortedBy { it.versie }
    }

    fun onStartup(@Observes @Initialized(ApplicationScoped::class) @Suppress("UNUSED_PARAMETER") event: Any) {
        solrClient = solrClientFactory.createSolrClient(SOLR_CORE)
        waitForSolrAvailability()
        try {
            val currentVersion = getCurrentVersion()
            val isUpToDate = currentVersion == schemaUpdates.last().versie
            LOG.info(SolrCoreVersionChecked(solrCore = SOLR_CORE, currentVersion = currentVersion, isUpToDate = isUpToDate))
            if (!isUpToDate) {
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
                logSolrCoreNotAvailable(solrServerException)
            } catch (ioException: IOException) {
                logSolrCoreNotAvailable(ioException)
            } catch (solrException: SolrException) {
                logSolrCoreNotAvailable(solrException)
            }
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
            ?.let { it.substringAfter(VERSION_FIELD_PREFIX).toInt() }
            ?: 0

    private fun logSolrCoreNotAvailable(exception: Exception) =
        LOG.info(
            SolrCoreNotAvailable(
                solrCore = SOLR_CORE,
                errorType = exception.javaClass.name,
                waitSeconds = WAIT_FOR_SOLR_SECONDS
            )
        )

    private fun apply(schemaUpdate: SolrSchemaUpdate) {
        LOG.info(SolrCoreUpdateStarted(solrCore = SOLR_CORE, version = schemaUpdate.versie))
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
        indexingService.reindexAllAsync(types)
    }
}
