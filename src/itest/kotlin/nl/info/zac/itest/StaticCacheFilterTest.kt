/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.BEHEERDER_ELK_ZAAKTYPE
import nl.info.zac.itest.config.ItestConfiguration.ZAC_BASE_URI
import java.net.HttpURLConnection.HTTP_OK

private val HASHED_SCRIPT_REGEX = Regex("""src="([^"]*-[A-Za-z0-9]{8}\.js)"""")

class StaticCacheFilterTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()

    Given("index.html") {
        When("the file is requested") {
            val response = itestHttpClient.performGetRequest("$ZAC_BASE_URI/index.html", testUser = BEHEERDER_ELK_ZAAKTYPE)
            Then("the response is 200 and Cache-Control contains no-cache") {
                response.code shouldBe HTTP_OK
                // The OIDC layer may append ', no-store, must-revalidate' at exchange level on top of
                // the filter's 'no-cache' value, so we check containment rather than exact equality.
                response.headers["Cache-Control"] shouldContain "no-cache"
            }
        }
    }

    Given("the root path") {
        When("the path is requested") {
            val response = itestHttpClient.performGetRequest("$ZAC_BASE_URI/", testUser = BEHEERDER_ELK_ZAAKTYPE)
            Then("the response is 200 and Cache-Control contains no-cache") {
                response.code shouldBe HTTP_OK
                response.headers["Cache-Control"] shouldContain "no-cache"
            }
        }
    }

    Given("a hashed JS bundle referenced from index.html") {
        val indexBody = itestHttpClient.performGetRequest(
            "$ZAC_BASE_URI/index.html",
            testUser = BEHEERDER_ELK_ZAAKTYPE
        ).bodyAsString
        val scriptName = requireNotNull(HASHED_SCRIPT_REGEX.find(indexBody)?.groupValues?.get(1)) {
            "Could not find a hashed JS bundle URL in index.html"
        }
        When("the bundle is requested") {
            val response = itestHttpClient.performGetRequest("$ZAC_BASE_URI/$scriptName", testUser = BEHEERDER_ELK_ZAAKTYPE)
            Then("the response is 200 with Cache-Control: immutable") {
                response.code shouldBe HTTP_OK
                response.headers["Cache-Control"] shouldBe "public, max-age=31536000, immutable"
            }
        }
    }

    Given("a versioned asset with a valid 8-character hex v parameter") {
        When("the asset is requested with ?v=395afa0f") {
            val response = itestHttpClient.performGetRequest(
                "$ZAC_BASE_URI/assets/i18n/nl.json?v=395afa0f",
                testUser = BEHEERDER_ELK_ZAAKTYPE
            )
            Then("Cache-Control is set to immutable") {
                response.headers["Cache-Control"] shouldBe "public, max-age=31536000, immutable"
            }
        }
    }

    Given("a versioned asset with an invalid v parameter") {
        When("the asset is requested with ?v=toolongval") {
            val response = itestHttpClient.performGetRequest(
                "$ZAC_BASE_URI/assets/i18n/nl.json?v=toolongval",
                testUser = BEHEERDER_ELK_ZAAKTYPE
            )
            Then("Cache-Control is not set to immutable") {
                response.headers["Cache-Control"]?.shouldNotContain("immutable")
            }
        }
    }

    Given("a REST API path") {
        When("the health endpoint is requested") {
            val response = itestHttpClient.performGetRequest("$ZAC_BASE_URI/rest/health", testUser = BEHEERDER_ELK_ZAAKTYPE)
            Then("Cache-Control is not set to immutable") {
                response.headers["Cache-Control"]?.shouldNotContain("immutable")
            }
        }
    }
})
