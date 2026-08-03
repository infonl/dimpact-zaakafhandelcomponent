/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.solr

import jakarta.annotation.Resource
import jakarta.enterprise.concurrent.ManagedExecutorService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.Initialized
import jakarta.enterprise.event.Observes
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.atos.zac.solr.FieldType.STRING
import net.atos.zac.solr.SolrSchemaUpdateHelper.NAME
import net.atos.zac.solr.SolrSchemaUpdateHelper.addField
import net.atos.zac.solr.SolrSchemaUpdateHelper.deleteField
import nl.info.zac.search.IndexingService
import nl.info.zac.search.IndexingService.Companion.SOLR_CORE
import nl.info.zac.search.model.zoekobject.ZoekObjectType
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
import java.util.logging.Level
import java.util.logging.Logger

@Singleton
@AllOpen
@NoArgConstructor
class SolrDeployerService @Inject constructor(
    @ConfigProperty(name = "SOLR_URL") private val solrUrl: String,
    private val indexingService: IndexingService
) {
    companion object {
        private val LOG = Logger.getLogger(SolrDeployerService::class.java.name)
        private const val VERSION_FIELD_PREFIX = "schema_version_"
        private const val SOLR_STATUS_OK = 0
        private const val INITIAL_WAIT_FOR_SOLR_SECONDS = 1L
        private const val MAX_WAIT_FOR_SOLR_SECONDS = 30L
        private const val WAIT_FOR_SOLR_BACKOFF_MULTIPLIER = 2L
    }

    private lateinit var managedExecutor: ManagedExecutorService
    private lateinit var schemaUpdates: List<SolrSchemaUpdate>
    private lateinit var solrClient: SolrClient

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
        val currentVersion = getCurrentVersion()
        LOG.info { "Current version of Solr core '$SOLR_CORE': $currentVersion" }
        val lastVersion = schemaUpdates.last().versie
        if (currentVersion == lastVersion) {
            LOG.info { "Solr core '$SOLR_CORE' is up to date. No Solr schema migration needed." }
        } else {
            schemaUpdates.drop(currentVersion).forEach(::apply)
            schemaUpdates.drop(currentVersion)
                .flatMap { it.teHerindexerenZoekObjectTypes }
                .toSet()
                .forEach(::startReindexing)
        }
    }

    // Retries with capped exponential backoff instead of a fixed 1-second interval: rootless-networking
    // hiccups between containers (e.g. under Podman) can keep Solr unreachable for well over a minute even
    // though it is otherwise healthy, and hammering the ping every second just adds log noise while that
    // resolves itself. There is no retry limit: Solr is a hard dependency, so waiting indefinitely is correct.
    private fun waitForSolrAvailability() {
        var waitSeconds = INITIAL_WAIT_FOR_SOLR_SECONDS
        while (true) {
            try {
                if (SolrPing().setActionPing().process(solrClient).status == SOLR_STATUS_OK) {
                    return
                }
            } catch (exception: SolrServerException) {
                LOG.info { "Solr core is not available yet. Exception: ${exception.message}" }
            } catch (exception: IOException) {
                LOG.info { "Solr core is not available yet. Exception: ${exception.message}" }
            } catch (exception: SolrException) {
                LOG.info { "Solr core is not available yet. Exception: ${exception.message}" }
            }
            LOG.info { "Waiting for $waitSeconds seconds for Solr core '$SOLR_CORE' to become available..." }
            try {
                Thread.sleep(Duration.ofSeconds(waitSeconds).toMillis())
            } catch (exception: InterruptedException) {
                LOG.log(
                    Level.WARNING,
                    "Thread was interrupted while waiting for Solr core to become available. Re-interrupting thread.",
                    exception
                )
                Thread.currentThread().interrupt()
            }
            waitSeconds = (waitSeconds * WAIT_FOR_SOLR_BACKOFF_MULTIPLIER).coerceAtMost(MAX_WAIT_FOR_SOLR_SECONDS)
        }
    }

    private fun getCurrentVersion(): Int =
        SchemaRequest.Fields().process(solrClient).fields
            .map { it[NAME].toString() }
            .firstOrNull { it.startsWith(VERSION_FIELD_PREFIX) }
            ?.let { StringUtils.substringAfter(it, VERSION_FIELD_PREFIX).toInt() }
            ?: 0

    private fun apply(schemaUpdate: SolrSchemaUpdate) {
        LOG.info { "Updating Solr core '$SOLR_CORE' to version: ${schemaUpdate.versie}" }
        val updates = schemaUpdate.schemaUpdates + updateVersionField(schemaUpdate.versie)
        SchemaRequest.MultiUpdate(updates).process(solrClient)
    }

    private fun updateVersionField(version: Int): List<SchemaRequest.Update> {
        val updates = mutableListOf<SchemaRequest.Update>()
        if (version > 1) {
            updates.add(deleteField(VERSION_FIELD_PREFIX + (version - 1)))
        }
        updates.add(addField(VERSION_FIELD_PREFIX + version, STRING, false, false))
        return updates
    }

    private fun startReindexing(type: ZoekObjectType) {
        managedExecutor.submit { indexingService.reindex(type) }
    }
}
