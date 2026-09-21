/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.solr

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.apache.solr.client.solrj.impl.Http2SolrClient
import org.eclipse.microprofile.config.inject.ConfigProperty

/**
 * Single place where Solr clients are created, so that every connection ZAC makes to Solr is
 * authenticated. The credentials are required configuration, so ZAC fails to start rather than
 * falling back to unauthenticated requests.
 */
@ApplicationScoped
@NoArgConstructor
@AllOpen
class SolrClientFactory @Inject constructor(
    @ConfigProperty(name = "SOLR_URL") private val solrUrl: String,
    @ConfigProperty(name = "SOLR_USERNAME") private val solrUsername: String,
    @ConfigProperty(name = "SOLR_PASSWORD") private val solrPassword: String
) {
    fun createSolrClient(core: String) =
        Http2SolrClient.Builder("$solrUrl/solr/$core")
            .withBasicAuthCredentials(solrUsername, solrPassword)
            .build()
}
