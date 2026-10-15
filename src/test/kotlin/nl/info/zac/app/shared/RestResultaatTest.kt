/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.shared

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

class RestResultaatTest : BehaviorSpec({
    context("Constructing a REST resultaat") {
        given("results without a total") {
            `when`("the resultaat is constructed") {
                val restResultaat = RestResultaat(listOf("fakeResult1", "fakeResult2"))

                then("the total is the number of results and there is no error message") {
                    restResultaat.totaal shouldBe 2f
                    restResultaat.foutmelding shouldBe ""
                }
            }
        }

        given("results with a total that differs from the number of results") {
            `when`("the resultaat is constructed") {
                val restResultaat = RestResultaat(listOf("fakeResult1"), 10)

                then("the given total is used") {
                    restResultaat.totaal shouldBe 10f
                }
            }
        }

        given("no results") {
            `when`("the resultaat is constructed") {
                val restResultaat = RestResultaat<String>()

                then("it has no results and a total of zero") {
                    restResultaat.resultaten.shouldBeEmpty()
                    restResultaat.totaal shouldBe 0f
                }
            }
        }
    }
})
