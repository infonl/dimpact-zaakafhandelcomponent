/*
 * SPDX-FileCopyrightText: 2024, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.solr

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.verify
import jakarta.enterprise.concurrent.ManagedExecutorService
import jakarta.enterprise.inject.Instance
import nl.info.zac.search.IndexingService
import nl.info.zac.search.IndexingService.Companion.SOLR_CORE
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.solr.exception.SolrDeploymentException
import org.apache.solr.client.solrj.SolrServerException
import org.apache.solr.client.solrj.request.SolrPing
import org.apache.solr.client.solrj.request.schema.SchemaRequest
import org.apache.solr.client.solrj.request.schema.SchemaRequest.Fields
import org.apache.solr.client.solrj.request.schema.SchemaRequest.MultiUpdate
import java.util.concurrent.CompletableFuture

class SolrDeployerServiceTest : BehaviorSpec({
    val managedExecutorService = mockk<ManagedExecutorService>()
    val indexingService = mockk<IndexingService>()
    val solrUrl = "https://example.com/solr"

    val solrDeployerService = SolrDeployerService(
        solrUrl,
        indexingService,
    )

    afterEach {
        checkUnnecessaryStub()
    }

    given(
        """
            ZAC Solr schema version 0 is currently installed and version 1 is available for which all zaken need to be reindexed
            """
    ) {
        mockkConstructor(SolrPing::class)
        // mock a successful ping
        every { anyConstructed<SolrPing>().setActionPing().process(any()).status } returns 0
        mockkConstructor(Fields::class)
        // mock that the current Solr schema version is '0' by returning an empty list
        every { anyConstructed<Fields>().process(any()).fields } returns emptyList()
        val solrSchemaUpdateInstance = mockk<Instance<SolrSchemaUpdate>>()
        val solrSchemaUpdate = mockk<SolrSchemaUpdate>()
        val solrSchemaRequestUpdate = mockk<SchemaRequest.Update>()
        every { solrSchemaUpdateInstance.iterator() } returns mutableListOf(solrSchemaUpdate).iterator()
        every { solrSchemaUpdate.versie } returns 1
        every { solrSchemaUpdate.schemaUpdates } returns listOf(solrSchemaRequestUpdate)
        mockkConstructor(MultiUpdate::class)
        every { anyConstructed<MultiUpdate>().process(any()) } returns null
        every { solrSchemaUpdate.teHerindexerenZoekObjectTypes } returns setOf(ZoekObjectType.ZAAK)
        every { managedExecutorService.submit(any()) } returns CompletableFuture.completedFuture(null)

        // prepare the SolrDeployerService by setting the executor service and the available schema updates
        solrDeployerService.setManagedExecutorService(managedExecutorService)
        solrDeployerService.setSchemaUpdates(solrSchemaUpdateInstance)

        `when`("the ZAC Solr deployer service is started") {
            solrDeployerService.onStartup(Any())

            then("the Solr schema should be updated to the available version and the zaken should be reindexed") {
                verify(exactly = 1) {
                    anyConstructed<MultiUpdate>().process(any())
                    managedExecutorService.submit(any())
                }
            }
        }
    }

    given("Solr is not available yet and the thread waiting for it becomes interrupted") {
        mockkConstructor(SolrPing::class)
        every { anyConstructed<SolrPing>().setActionPing().process(any()) } throws
            SolrServerException("Solr core is not available")

        `when`("the ZAC Solr deployer service is started and gets interrupted while waiting for Solr") {
            var caughtException: Throwable? = null
            val startupThread = Thread {
                try {
                    solrDeployerService.onStartup(Any())
                } catch (throwable: Throwable) {
                    caughtException = throwable
                }
            }
            startupThread.start()
            while (startupThread.state != Thread.State.TIMED_WAITING) {
                Thread.sleep(10)
            }
            startupThread.interrupt()
            startupThread.join(5_000)

            then("a SolrDeploymentException is thrown, propagating the interruption as a deployment failure") {
                caughtException.shouldBeInstanceOf<SolrDeploymentException>()
                caughtException?.message shouldBe
                    "Thread was interrupted while waiting for Solr core '$SOLR_CORE' to become available"
                caughtException?.cause.shouldBeInstanceOf<InterruptedException>()
            }
        }
    }
})
