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
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import nl.info.zac.search.IndexingService
import nl.info.zac.search.IndexingService.Companion.SOLR_CORE
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.solr.exception.SolrDeploymentException
import org.apache.solr.client.solrj.SolrServerException
import org.apache.solr.client.solrj.request.SolrPing
import org.apache.solr.client.solrj.request.schema.SchemaRequest
import org.apache.solr.client.solrj.request.schema.SchemaRequest.Fields
import org.apache.solr.client.solrj.request.schema.SchemaRequest.MultiUpdate

class SolrDeployerServiceTest : BehaviorSpec({
    val indexingService = mockk<IndexingService>()
    val solrUrl = "https://example.com/solr"
    val testDispatcher = StandardTestDispatcher()

    val solrDeployerService = SolrDeployerService(
        solrUrl,
        indexingService,
        testDispatcher,
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
        every { indexingService.reindexAll(any()) } just runs

        // prepare the SolrDeployerService by setting the available schema updates
        solrDeployerService.setSchemaUpdates(solrSchemaUpdateInstance)

        `when`("the ZAC Solr deployer service is started") {
            runTest(testDispatcher) {
                solrDeployerService.onStartup(Any())
            }

            then(
                """the Solr schema should be updated to the available version and the complete reindexing
                   process should be triggered for the zaaktypes that need reindexing"""
            ) {
                verify(exactly = 1) {
                    anyConstructed<MultiUpdate>().process(any())
                    indexingService.reindexAll(setOf(ZoekObjectType.ZAAK))
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
