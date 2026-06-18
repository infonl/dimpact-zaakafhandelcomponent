/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.health

import co.elastic.clients.elasticsearch.ElasticsearchClient
import io.opentelemetry.instrumentation.annotations.WithSpan
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import nl.info.zac.search.elasticsearch.SearchIndex
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.health.HealthCheck
import org.eclipse.microprofile.health.HealthCheckResponse
import org.eclipse.microprofile.health.Readiness
import java.time.LocalDateTime

/**
 * Readiness probe for the search backend. Reports UP only when the Elasticsearch cluster is reachable and all
 * per-type search indices (zaak, taak, document) are available.
 */
@Readiness
@ApplicationScoped
@AllOpen
@NoArgConstructor
class ElasticsearchReadinessHealthCheck @Inject constructor(
    private val elasticsearchClient: ElasticsearchClient
) : HealthCheck {

    @WithSpan(value = "GET ElasticsearchReadinessHealthCheck")
    override fun call(): HealthCheckResponse {
        val responseBuilder = HealthCheckResponse.named(ElasticsearchReadinessHealthCheck::class.java.name)
        return try {
            val missingIndices = SearchIndex.entries.filterNot { searchIndex ->
                elasticsearchClient.indices().exists { it.index(searchIndex.indexName) }.value()
            }
            val clusterAvailable = elasticsearchClient.ping().value()
            if (clusterAvailable && missingIndices.isEmpty()) {
                responseBuilder
                    .withData("indices", SearchIndex.ALL_INDEX_NAMES.joinToString(","))
                    .up()
                    .build()
            } else {
                responseBuilder
                    .withData("time", LocalDateTime.now().toString())
                    .withData("clusterAvailable", clusterAvailable)
                    .withData("missingIndices", missingIndices.joinToString(",") { it.indexName })
                    .down()
                    .build()
            }
        } catch (@Suppress("TooGenericExceptionCaught") exception: Throwable) {
            responseBuilder
                .withData("time", LocalDateTime.now().toString())
                .withData("error", exception.message ?: exception.javaClass.name)
                .down()
                .build()
        }
    }
}
