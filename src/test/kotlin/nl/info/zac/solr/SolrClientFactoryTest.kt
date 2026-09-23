/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.solr

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.apache.solr.client.solrj.impl.HttpSolrClientBase

class SolrClientFactoryTest : BehaviorSpec({
    given("a configured Solr URL, username and password") {
        val solrClientFactory = SolrClientFactory(
            solrUrl = "https://example.com",
            solrUsername = "fakeSolrUsername",
            solrPassword = "fakeSolrPassword"
        )

        `when`("a Solr client is created") {
            solrClientFactory.createSolrClient("fakeSolrCore").use { solrClient ->
                then("the client sends the configured basic authentication credentials") {
                    val basicAuthAuthorizationStr = HttpSolrClientBase::class.java
                        .getDeclaredField("basicAuthAuthorizationStr")
                        .apply { isAccessible = true }
                        .get(solrClient)

                    basicAuthAuthorizationStr shouldBe "Basic ZmFrZVNvbHJVc2VybmFtZTpmYWtlU29sclBhc3N3b3Jk"
                }
            }
        }
    }
})
