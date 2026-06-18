/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.search.elasticsearch

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.rest_client.RestClientTransport
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Disposes
import jakarta.enterprise.inject.Produces
import jakarta.inject.Inject
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.impl.client.BasicCredentialsProvider
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.elasticsearch.client.RestClient
import java.util.Optional
import java.util.logging.Logger

/**
 * Produces the singleton [ElasticsearchClient] used by the indexing and search services.
 *
 * Connection is configured through the `elasticsearch.url` MicroProfile property (env `ELASTICSEARCH_URL`),
 * with optional basic-auth credentials (`ELASTICSEARCH_USERNAME` / `ELASTICSEARCH_PASSWORD`) or an API key
 * (`ELASTICSEARCH_API_KEY`).
 */
@ApplicationScoped
class ElasticsearchClientProducer @Inject constructor(
    @ConfigProperty(name = "elasticsearch.url", defaultValue = "http://localhost:9200")
    private val elasticsearchUrl: String,

    @ConfigProperty(name = "elasticsearch.username")
    private val username: Optional<String>,

    @ConfigProperty(name = "elasticsearch.password")
    private val password: Optional<String>,

    @ConfigProperty(name = "elasticsearch.apiKey")
    private val apiKey: Optional<String>
) {
    companion object {
        private val LOG = Logger.getLogger(ElasticsearchClientProducer::class.java.name)

        /**
         * Shared [ObjectMapper] used to (de)serialize the `*ZoekObject` documents and the Elasticsearch
         * request/response bodies. Dates are written as ISO-8601 strings so that the index `date` mapping
         * can parse them.
         */
        fun createObjectMapper(): ObjectMapper =
            ObjectMapper()
                .registerKotlinModule()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                // Bind to fields (like the former SolrJ field binding) so the private `id`/`type` fields are
                // (de)serialized and the getType()/getObjectId() overrides are not mistaken for properties.
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
                .setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE)
                .setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE)
    }

    @Produces
    @ApplicationScoped
    fun produceElasticsearchClient(): ElasticsearchClient {
        LOG.info("Creating Elasticsearch client for URL '$elasticsearchUrl'")
        val restClientBuilder = RestClient.builder(HttpHost.create(elasticsearchUrl))

        if (username.isPresent && password.isPresent) {
            val credentialsProvider = BasicCredentialsProvider().apply {
                setCredentials(AuthScope.ANY, UsernamePasswordCredentials(username.get(), password.get()))
            }
            restClientBuilder.setHttpClientConfigCallback { it.setDefaultCredentialsProvider(credentialsProvider) }
        } else if (apiKey.isPresent) {
            val authHeader = org.apache.http.message.BasicHeader("Authorization", "ApiKey ${apiKey.get()}")
            restClientBuilder.setDefaultHeaders(arrayOf(authHeader))
        }

        val transport = RestClientTransport(restClientBuilder.build(), JacksonJsonpMapper(createObjectMapper()))
        return ElasticsearchClient(transport)
    }

    fun closeElasticsearchClient(@Disposes client: ElasticsearchClient) {
        runCatching { client._transport().close() }
            .onFailure { LOG.warning("Failed to close Elasticsearch client transport: ${it.message}") }
    }

    @PreDestroy
    fun cleanup() {
        // transport closed via the @Disposes disposer when the application scope ends
    }
}
