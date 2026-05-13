/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.health

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import nl.info.zac.search.IndexingService.Companion.SOLR_CORE
import org.apache.solr.client.solrj.SolrClient
import org.apache.solr.client.solrj.SolrServerException
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.client.solrj.request.SolrPing
import org.apache.solr.client.solrj.response.SolrPingResponse
import org.eclipse.microprofile.health.HealthCheckResponse

class SolrReadinessHealthCheckTest : BehaviorSpec({
    val solrUrl = "http://localhost:8983"
    lateinit var solrClient: SolrClient
    lateinit var solrPing: SolrPing
    lateinit var solrPingResponse: SolrPingResponse

    beforeTest {
        // Initialize the mock SolrClient
        mockkConstructor(Http2SolrClient.Builder::class)
        solrClient = mockk<Http2SolrClient>(relaxed = true)
        every { anyConstructed<Http2SolrClient.Builder>().build() } returns solrClient

        // Initialize the mock SolrPing and mock SolrPingResponse
        mockkConstructor(SolrPing::class)
        solrPingResponse = mockk()
        solrPing = mockk<SolrPing> {
            every { setActionPing() } returns this
            every { process(any()) } returns solrPingResponse
        }
        every { anyConstructed<SolrPing>().setActionPing() } returns solrPing
    }

    afterEach {
        clearAllMocks()
    }

    Given("Solr is available and returns status 0") {
        When("the health check is called") {
            every { solrPingResponse.status } returns 0
            val response = SolrReadinessHealthCheck(solrUrl).call()

            Then("the health check should return UP status") {
                response.status shouldBe HealthCheckResponse.Status.UP
                response.name shouldBe "nl.info.zac.health.SolrReadinessHealthCheck"

                with(response.data.get()) {
                    get("core") shouldBe SOLR_CORE
                    get("status") shouldBe 0L
                }
            }
        }
    }

    Given("Solr is responding but returns a non-zero status") {
        listOf(1, -1).forEach { status ->
            When("the health check is called with non-zero status $status") {
                every { solrPingResponse.status } returns status
                val response = SolrReadinessHealthCheck(solrUrl).call()

                Then("the health check should return DOWN status") {
                    response.status shouldBe HealthCheckResponse.Status.DOWN

                    with(response.data.get()) {
                        get("core") shouldBe SOLR_CORE
                        get("status") shouldBe status
                        get("error") shouldBe "Solr ping returned non-zero status"
                        containsKey("time") shouldBe true
                    }
                }
            }
        }
    }

    Given("Solr is not available and throws an exception") {
        listOf<Throwable>(
            SolrServerException("Connection refused to Solr server"),
            RuntimeException("Network timeout")
        ).forEach { err ->
            When("the health check is called and error ${err.javaClass.simpleName} is thrown") {
                every { solrPingResponse.status } throws err
                val response = SolrReadinessHealthCheck(solrUrl).call()

                Then("the health check should return DOWN status with error") {
                    response.status shouldBe HealthCheckResponse.Status.DOWN

                    with(response.data.get()) {
                        get("core") shouldBe SOLR_CORE
                        get("error") shouldBe err.message
                        containsKey("time") shouldBe true
                        containsKey("status") shouldBe false
                    }
                }
            }
        }
    }
})
