/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.shared

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import jakarta.json.bind.JsonbBuilder

class RestResultaatTest : BehaviorSpec({
    context("Constructing a REST resultaat") {
        given("results without a total") {
            `when`("the resultaat is constructed") {
                val restResultaat = RestResultaat(listOf("fakeResult1", "fakeResult2"))

                then("the total is the number of results and there is no error message") {
                    restResultaat.resultCount shouldBe 2L
                    restResultaat.errorMessage shouldBe ""
                }
            }
        }

        given("results with a total that differs from the number of results") {
            `when`("the resultaat is constructed") {
                val restResultaat = RestResultaat(listOf("fakeResult1"), 10)

                then("the given total is used") {
                    restResultaat.resultCount shouldBe 10L
                }
            }
        }

        given("no results") {
            `when`("the resultaat is constructed") {
                val restResultaat = RestResultaat<String>()

                then("it has no results and a total of zero") {
                    restResultaat.results.shouldBeEmpty()
                    restResultaat.resultCount shouldBe 0L
                }
            }
        }
    }

    context("Serializing a REST resultaat to JSON") {
        given("a resultaat with results") {
            val restResultaat = RestResultaat(listOf("fakeResult"))

            `when`("it is serialized") {
                val json = JsonbBuilder.create().use { it.toJson(restResultaat) }

                then("the JSON uses the Dutch field names that the frontend expects") {
                    json shouldBe """{"foutmelding":"","resultaten":["fakeResult"],"totaal":1}"""
                }
            }
        }
    }
})
