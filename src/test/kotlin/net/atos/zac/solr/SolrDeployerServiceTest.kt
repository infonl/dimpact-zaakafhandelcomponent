/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.solr

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.verify
import jakarta.enterprise.concurrent.ManagedExecutorService
import jakarta.enterprise.inject.Instance
import net.atos.zac.search.IndexingService
import net.atos.zac.search.model.zoekobject.ZoekObjectType
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

    beforeEach {
        checkUnnecessaryStub()
    }

    Given(
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
        every { solrSchemaUpdateInstance.stream().sorted(any()).toList() } returns listOf(solrSchemaUpdate)
        every { solrSchemaUpdate.versie } returns 1
        every { solrSchemaUpdate.schemaUpdates } returns listOf(solrSchemaRequestUpdate)
        mockkConstructor(MultiUpdate::class)
        every { anyConstructed<MultiUpdate>().process(any()) } returns null
        every { solrSchemaUpdate.teHerindexerenZoekObjectTypes } returns setOf(ZoekObjectType.ZAAK)
        every { managedExecutorService.submit(any()) } returns CompletableFuture.completedFuture(null)

        // prepare the SolrDeployerService by setting the executor service and the available schema updates
        solrDeployerService.setManagedExecutorService(managedExecutorService)
        solrDeployerService.setSchemaUpdates(solrSchemaUpdateInstance)

        When("the ZAC Solr deployer service is started") {
            solrDeployerService.onStartup(Object())

            Then("the Solr schema should be updated to the available version and the zaken should be reindexed") {
                verify(exactly = 1) {
                    anyConstructed<MultiUpdate>().process(any())
                    managedExecutorService.submit(any())
                }
            }
        }
    }
})
