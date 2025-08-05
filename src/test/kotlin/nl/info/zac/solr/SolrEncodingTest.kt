/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.solr

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class SolrEncodingTest : BehaviorSpec({

    Given("a string without Solr special characters") {
        val input = "abc123"

        When("encoded") {
            val result = encoded(input)

            Then("it should return the original string") {
                result shouldBe input
            }
        }

        When("quoted") {
            val result = quoted(input)

            Then("it should return the string wrapped in quotes") {
                result shouldBe "\"$input\""
            }
        }
    }

    Given("a string with a colon") {
        val input = "key:value"

        When("encoded") {
            val result = encoded(input)

            Then("it should escape the colon") {
                result shouldBe "key\\:value"
            }
        }

        When("quoted") {
            val result = quoted(input)

            Then("it should return quoted and escaped") {
                result shouldBe "\"key\\:value\""
            }
        }
    }

    Given("a string with multiple Solr special characters") {
        val input = "+hello:(world)?"
        val expectedEncoded = "\\+hello\\:\\(world\\)\\?"

        When("encoded") {
            val result = encoded(input)

            Then("it should escape all special characters") {
                result shouldBe expectedEncoded
            }
        }

        When("quoted") {
            val result = quoted(input)

            Then("it should return quoted and escaped string") {
                result shouldBe "\"$expectedEncoded\""
            }
        }
    }

    Given("an empty string") {
        val input = ""

        When("encoded") {
            val result = encoded(input)

            Then("it should return an empty string") {
                result shouldBe ""
            }
        }

        When("quoted") {
            val result = quoted(input)

            Then("it should return a pair of quotes") {
                result shouldBe "\"\""
            }
        }
    }
})
