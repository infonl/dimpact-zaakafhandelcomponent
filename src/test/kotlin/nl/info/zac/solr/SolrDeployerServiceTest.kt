/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.solr

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import io.mockk.verify
import jakarta.enterprise.concurrent.ManagedExecutorService
import jakarta.enterprise.inject.Instance
import net.atos.zac.solr.SolrSchemaUpdate
import nl.info.zac.search.IndexingService
import nl.info.zac.search.model.zoekobject.ZoekObjectType
import nl.info.zac.solr.SolrDeployerService.Companion.SOLR_COLLECTION_A
import org.apache.solr.client.solrj.request.CollectionAdminRequest
import org.apache.solr.client.solrj.request.SolrPing
import org.apache.solr.client.solrj.request.schema.SchemaRequest
import org.apache.solr.client.solrj.request.schema.SchemaRequest.Fields
import org.apache.solr.client.solrj.request.schema.SchemaRequest.MultiUpdate
import org.apache.solr.client.solrj.response.CollectionAdminResponse
import org.apache.solr.common.util.NamedList
import java.util.concurrent.CompletableFuture

class SolrDeployerServiceTest : BehaviorSpec({
    val managedExecutorService = mockk<ManagedExecutorService>()
    val indexingService = mockk<IndexingService>()
    val solrUrl = "https://solr.example.com"

    val solrDeployerService = SolrDeployerService(
        solrUrl = solrUrl,
        numShards = 1,
        replicationFactor = 1,
        indexingService = indexingService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    afterEach {
        unmockkConstructor(
            CollectionAdminRequest.List::class,
            CollectionAdminRequest.ListAliases::class,
            CollectionAdminRequest.Create::class,
            CollectionAdminRequest.CreateAlias::class,
            CollectionAdminRequest.Delete::class,
            SolrPing::class,
            Fields::class,
            MultiUpdate::class
        )
    }

    Given(
        """
        SolrCloud mode, no alias exists yet, schema version 0 installed, version 1 available requiring ZAAK reindex
        """
    ) {
        // Collections API available (SolrCloud mode)
        mockkConstructor(CollectionAdminRequest.List::class)
        every { anyConstructed<CollectionAdminRequest.List>().process(any()) } returns
            createCollectionListResponse(emptyList())

        // Alias check: first call returns empty (no alias), second call returns zac -> zac_a
        mockkConstructor(CollectionAdminRequest.ListAliases::class)
        every { anyConstructed<CollectionAdminRequest.ListAliases>().process(any()) } returnsMany listOf(
            createAliasResponse(emptyMap()),
            createAliasResponse(mapOf(nl.info.zac.search.IndexingService.SOLR_CORE to SOLR_COLLECTION_A))
        )

        // Collection and alias creation
        mockkConstructor(CollectionAdminRequest.Create::class)
        every { anyConstructed<CollectionAdminRequest.Create>().process(any()) } returns mockk()

        mockkConstructor(CollectionAdminRequest.CreateAlias::class)
        every { anyConstructed<CollectionAdminRequest.CreateAlias>().process(any()) } returns mockk()

        // SolrPing for collection availability check
        mockkConstructor(SolrPing::class)
        every { anyConstructed<SolrPing>().setActionPing().process(any()).status } returns 0

        // Schema version: 0 (empty field list)
        mockkConstructor(Fields::class)
        every { anyConstructed<Fields>().process(any()).fields } returns emptyList()

        val solrSchemaUpdateInstance = mockk<Instance<SolrSchemaUpdate>>()
        val solrSchemaUpdate = mockk<SolrSchemaUpdate>()
        val solrSchemaRequestUpdate = mockk<SchemaRequest.Update>()
        every { solrSchemaUpdateInstance.stream().sorted(any()).toList() } returns listOf(solrSchemaUpdate)
        every { solrSchemaUpdate.versie } returns 1
        every { solrSchemaUpdate.schemaUpdates } returns listOf(solrSchemaRequestUpdate)
        every { solrSchemaUpdate.teHerindexerenZoekObjectTypes } returns setOf(ZoekObjectType.ZAAK)

        // Schema update
        mockkConstructor(MultiUpdate::class)
        every { anyConstructed<MultiUpdate>().process(any()) } returns null

        // Executor: run tasks inline (synchronously for testability)
        every { managedExecutorService.submit(any<Runnable>()) } returns CompletableFuture.completedFuture(null)

        solrDeployerService.setManagedExecutorService(managedExecutorService)
        solrDeployerService.setSchemaUpdates(solrSchemaUpdateInstance)

        When("the Solr deployer service starts up") {
            solrDeployerService.onStartup(Any())

            Then(
                "schema is applied to the new inactive collection and two tasks are submitted: one reindex and one alias switch"
            ) {
                verify(exactly = 1) { anyConstructed<MultiUpdate>().process(any()) }
                // 1st submit: reindex ZAAK into zac_b; 2nd submit: alias switch + delete zac_a
                verify(exactly = 2) { managedExecutorService.submit(any<Runnable>()) }
            }
        }
    }

    Given(
        """
        Standalone (non-SolrCloud) Solr mode, schema version 0 installed, version 1 available requiring ZAAK reindex
        """
    ) {
        // Collections API returns HTTP 400 → standalone mode
        mockkConstructor(CollectionAdminRequest.List::class)
        every { anyConstructed<CollectionAdminRequest.List>().process(any()) } throws
            org.apache.solr.common.SolrException(
                org.apache.solr.common.SolrException.ErrorCode.BAD_REQUEST,
                "SolrCloud mode not enabled"
            )

        // SolrPing for collection availability check
        mockkConstructor(SolrPing::class)
        every { anyConstructed<SolrPing>().setActionPing().process(any()).status } returns 0

        // Schema version: 0
        mockkConstructor(Fields::class)
        every { anyConstructed<Fields>().process(any()).fields } returns emptyList()

        val solrSchemaUpdateInstance = mockk<Instance<SolrSchemaUpdate>>()
        val solrSchemaUpdate = mockk<SolrSchemaUpdate>()
        val solrSchemaRequestUpdate = mockk<SchemaRequest.Update>()
        every { solrSchemaUpdateInstance.stream().sorted(any()).toList() } returns listOf(solrSchemaUpdate)
        every { solrSchemaUpdate.versie } returns 1
        every { solrSchemaUpdate.schemaUpdates } returns listOf(solrSchemaRequestUpdate)
        every { solrSchemaUpdate.teHerindexerenZoekObjectTypes } returns setOf(ZoekObjectType.ZAAK)

        mockkConstructor(MultiUpdate::class)
        every { anyConstructed<MultiUpdate>().process(any()) } returns null

        every { managedExecutorService.submit(any<Runnable>()) } returns CompletableFuture.completedFuture(null)

        solrDeployerService.setManagedExecutorService(managedExecutorService)
        solrDeployerService.setSchemaUpdates(solrSchemaUpdateInstance)

        When("the Solr deployer service starts up") {
            solrDeployerService.onStartup(Any())

            Then("schema is applied directly to the alias collection and one reindex task is submitted") {
                verify(exactly = 1) { anyConstructed<MultiUpdate>().process(any()) }
                verify(exactly = 1) { managedExecutorService.submit(any<Runnable>()) }
            }
        }
    }

    Given(
        """
        SolrCloud mode, alias zac -> zac_a exists, schema is already at the latest version
        """
    ) {
        mockkConstructor(CollectionAdminRequest.List::class)
        // Use relaxed mock: alias already exists so collectionExists() (which accesses .response) is never called
        every { anyConstructed<CollectionAdminRequest.List>().process(any()) } returns mockk(relaxed = true)

        mockkConstructor(CollectionAdminRequest.ListAliases::class)
        every { anyConstructed<CollectionAdminRequest.ListAliases>().process(any()) } returns
            createAliasResponse(mapOf(nl.info.zac.search.IndexingService.SOLR_CORE to SOLR_COLLECTION_A))

        mockkConstructor(SolrPing::class)
        every { anyConstructed<SolrPing>().setActionPing().process(any()).status } returns 0

        // Schema already at version 1
        mockkConstructor(Fields::class)
        every { anyConstructed<Fields>().process(any()).fields } returns listOf(
            mapOf("name" to "schema_version_1")
        )

        val solrSchemaUpdateInstance = mockk<Instance<SolrSchemaUpdate>>()
        val solrSchemaUpdate = mockk<SolrSchemaUpdate>()
        every { solrSchemaUpdateInstance.stream().sorted(any()).toList() } returns listOf(solrSchemaUpdate)
        every { solrSchemaUpdate.versie } returns 1

        solrDeployerService.setManagedExecutorService(managedExecutorService)
        solrDeployerService.setSchemaUpdates(solrSchemaUpdateInstance)

        When("the Solr deployer service starts up") {
            solrDeployerService.onStartup(Any())

            Then("no schema update and no reindex task is submitted") {
                verify(exactly = 0) { managedExecutorService.submit(any<Runnable>()) }
            }
        }
    }
})

private fun createCollectionListResponse(collections: List<String>): CollectionAdminResponse {
    val response = mockk<CollectionAdminResponse>()
    val namedList = NamedList<Any>().apply { add("collections", collections) }
    every { response.response } returns namedList
    return response
}

private fun createAliasResponse(aliases: Map<String, String>): CollectionAdminResponse {
    val response = mockk<CollectionAdminResponse>()
    val aliasNamedList = NamedList<Any>().apply { aliases.forEach { (key, value) -> add(key, value) } }
    val outerNamedList = NamedList<Any>().apply { add("aliases", aliasNamedList) }
    every { response.response } returns outerNamedList
    return response
}
