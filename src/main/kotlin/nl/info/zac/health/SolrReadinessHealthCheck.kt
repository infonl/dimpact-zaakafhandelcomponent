/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.health

import io.opentelemetry.instrumentation.annotations.WithSpan
import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import nl.info.zac.search.IndexingService.Companion.SOLR_CORE
import nl.info.zac.util.AllOpen
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.client.solrj.request.SolrPing
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.health.HealthCheck
import org.eclipse.microprofile.health.HealthCheckResponse
import org.eclipse.microprofile.health.Readiness
import java.time.LocalDateTime

@Readiness
@ApplicationScoped
@AllOpen
class SolrReadinessHealthCheck @Inject constructor(
    @ConfigProperty(name = "SOLR_URL") private val solrUrl: String
) : HealthCheck {

    companion object {
        private const val SOLR_STATUS_OK = 0
    }

    private var solrClient: Http2SolrClient? = null

    @Synchronized
    private fun getSolrClient(): Http2SolrClient {
        if (solrClient == null) {
            solrClient = Http2SolrClient.Builder("$solrUrl/solr/${SOLR_CORE}").build()
        }
        return solrClient ?: error("solrClient should have been initialized")
    }

    @PreDestroy
    fun cleanup() {
        solrClient?.close()
    }

    @WithSpan(value = "GET SolrReadinessHealthCheck")
    override fun call(): HealthCheckResponse =
        try {
            val status = SolrPing().setActionPing().process(getSolrClient()).status
            if (status == SOLR_STATUS_OK) {
                HealthCheckResponse
                    .named(SolrReadinessHealthCheck::class.java.name)
                    .withData("core", SOLR_CORE)
                    .withData("status", SOLR_STATUS_OK.toLong())
                    .up()
                    .build()
            } else {
                HealthCheckResponse
                    .named(SolrReadinessHealthCheck::class.java.name)
                    .withData("time", LocalDateTime.now().toString())
                    .withData("core", SOLR_CORE)
                    .withData("status", status.toLong())
                    .withData("error", "Solr ping returned non-zero status")
                    .down()
                    .build()
            }
        } catch (@Suppress("TooGenericExceptionCaught") exception: Throwable) {
            HealthCheckResponse
                .named(SolrReadinessHealthCheck::class.java.name)
                .withData("time", LocalDateTime.now().toString())
                .withData("core", SOLR_CORE)
                .withData("error", exception.message)
                .down()
                .build()
        }
}
