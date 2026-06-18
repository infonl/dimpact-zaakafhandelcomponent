/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.health

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch.indices.ElasticsearchIndicesClient
import co.elastic.clients.elasticsearch.indices.ExistsRequest
import co.elastic.clients.transport.endpoints.BooleanResponse
import co.elastic.clients.util.ObjectBuilder
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.eclipse.microprofile.health.HealthCheckResponse
import java.util.function.Function

class ElasticsearchReadinessHealthCheckTest : BehaviorSpec({
    val elasticsearchClient = mockk<ElasticsearchClient>()
    val indicesClient = mockk<ElasticsearchIndicesClient>()
    val healthCheck = ElasticsearchReadinessHealthCheck(elasticsearchClient)

    fun stubIndicesExist(exist: Boolean) {
        every { elasticsearchClient.indices() } returns indicesClient
        every {
            indicesClient.exists(any<Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>>>())
        } returns BooleanResponse(exist)
    }

    Given("Elasticsearch is reachable and all indices exist") {
        When("the health check is called") {
            Then("the health check should return UP status") {
                stubIndicesExist(true)
                every { elasticsearchClient.ping() } returns BooleanResponse(true)

                val response = healthCheck.call()

                response.status shouldBe HealthCheckResponse.Status.UP
                response.name shouldBe "nl.info.zac.health.ElasticsearchReadinessHealthCheck"
                response.data.get()["indices"] shouldBe "zac-zaak,zac-taak,zac-document"
            }
        }
    }

    Given("Elasticsearch is reachable but one or more indices are missing") {
        When("the health check is called") {
            Then("the health check should return DOWN status listing the missing indices") {
                stubIndicesExist(false)
                every { elasticsearchClient.ping() } returns BooleanResponse(true)

                val response = healthCheck.call()

                response.status shouldBe HealthCheckResponse.Status.DOWN
                with(response.data.get()) {
                    get("missingIndices") shouldBe "zac-zaak,zac-taak,zac-document"
                    containsKey("time") shouldBe true
                }
            }
        }
    }

    Given("Elasticsearch is not reachable and throws an exception") {
        When("the health check is called") {
            Then("the health check should return DOWN status with the error") {
                every { elasticsearchClient.indices() } returns indicesClient
                every {
                    indicesClient.exists(any<Function<ExistsRequest.Builder, ObjectBuilder<ExistsRequest>>>())
                } throws RuntimeException("Connection refused")

                val response = healthCheck.call()

                response.status shouldBe HealthCheckResponse.Status.DOWN
                with(response.data.get()) {
                    get("error") shouldBe "Connection refused"
                    containsKey("time") shouldBe true
                }
            }
        }
    }
})
