/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.health

import io.opentelemetry.instrumentation.annotations.WithSpan
import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.apache.solr.client.solrj.request.SolrPing
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.eclipse.microprofile.health.HealthCheck
import org.eclipse.microprofile.health.HealthCheckResponse
import org.eclipse.microprofile.health.Readiness
import java.time.LocalDateTime

@Readiness
@ApplicationScoped
class SolrReadinessHealthCheck @Inject constructor(
    @ConfigProperty(name = "SOLR_URL") private val solrUrl: String
) : HealthCheck {

    companion object {
        private const val SOLR_STATUS_OK = 0
        private const val SOLR_CORE = "zac"
    }

    private val solrClient: Http2SolrClient by lazy {
        Http2SolrClient.Builder("$solrUrl/solr/$SOLR_CORE").build()
    }

    @PreDestroy
    fun cleanup() {
        solrClient.close()
    }

    @WithSpan(value = "GET SolrReadinessHealthCheck")
    override fun call(): HealthCheckResponse =
        try {
            val status = SolrPing().setActionPing().process(solrClient).status
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
        } catch (@Suppress("TooGenericExceptionCaught") exception: RuntimeException) {
            HealthCheckResponse
                .named(SolrReadinessHealthCheck::class.java.name)
                .withData("time", LocalDateTime.now().toString())
                .withData("core", SOLR_CORE)
                .withData("error", exception.message)
                .down()
                .build()
        }
}
