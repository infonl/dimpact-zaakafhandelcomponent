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
import nl.info.zac.itest.config.BEHEERDER_1
import nl.info.zac.itest.config.ItestConfiguration.ZAC_BASE_URI
import java.net.HttpURLConnection.HTTP_OK

private val HASHED_SCRIPT_REGEX = Regex("""src="([^"]*-[A-Za-z0-9]{8}\.js)"""")

class StaticCacheFilterTest : BehaviorSpec({
    val itestHttpClient = ItestHttpClient()

    given("index.html") {
        `when`("the file is requested") {
            val response = itestHttpClient.performGetRequest(
                "$ZAC_BASE_URI/index.html",
                testUser = BEHEERDER_1
            )
            then("the response is 200 and Cache-Control contains no-cache") {
                response.code shouldBe HTTP_OK
                // The OIDC layer may append ', no-store, must-revalidate' at exchange level on top of
                // the filter's 'no-cache' value, so we check containment rather than exact equality.
                response.headers["Cache-Control"] shouldContain "no-cache"
            }
        }
    }

    given("the root path") {
        `when`("the path is requested") {
            val response = itestHttpClient.performGetRequest("$ZAC_BASE_URI/", testUser = BEHEERDER_1)
            then("the response is 200 and Cache-Control contains no-cache") {
                response.code shouldBe HTTP_OK
                response.headers["Cache-Control"] shouldContain "no-cache"
            }
        }
    }

    given("a hashed JS bundle referenced from index.html") {
        val indexBody = itestHttpClient.performGetRequest(
            "$ZAC_BASE_URI/index.html",
            testUser = BEHEERDER_1
        ).bodyAsString
        val scriptName = requireNotNull(HASHED_SCRIPT_REGEX.find(indexBody)?.groupValues?.get(1)) {
            "Could not find a hashed JS bundle URL in index.html"
        }
        `when`("the bundle is requested") {
            val response = itestHttpClient.performGetRequest(
                "$ZAC_BASE_URI/$scriptName",
                testUser = BEHEERDER_1
            )
            then("the response is 200 with Cache-Control: immutable") {
                response.code shouldBe HTTP_OK
                response.headers["Cache-Control"] shouldBe "public, max-age=31536000, immutable"
            }
        }
    }

    given("a versioned asset with a valid 8-character hex v parameter") {
        `when`("the asset is requested with ?v=395afa0f") {
            val response = itestHttpClient.performGetRequest(
                "$ZAC_BASE_URI/assets/i18n/nl.json?v=395afa0f",
                testUser = BEHEERDER_1
            )
            then("Cache-Control is set to immutable") {
                response.headers["Cache-Control"] shouldBe "public, max-age=31536000, immutable"
            }
        }
    }

    given("a versioned asset with an invalid v parameter") {
        `when`("the asset is requested with ?v=toolongval") {
            val response = itestHttpClient.performGetRequest(
                "$ZAC_BASE_URI/assets/i18n/nl.json?v=toolongval",
                testUser = BEHEERDER_1
            )
            then("Cache-Control is not set to immutable") {
                response.headers["Cache-Control"]?.shouldNotContain("immutable")
            }
        }
    }

    given("a REST API path") {
        `when`("the health endpoint is requested") {
            val response = itestHttpClient.performGetRequest(
                "$ZAC_BASE_URI/rest/health",
                testUser = BEHEERDER_1
            )
            then("Cache-Control is not set to immutable") {
                response.headers["Cache-Control"]?.shouldNotContain("immutable")
            }
        }
    }
})
