/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.solr

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class SolrUtilTest : BehaviorSpec({

    given("a string without Solr special characters") {
        val input = "abc123"

        `when`("encoded") {
            val result = encoded(input)

            then("it should return the original string") {
                result shouldBe input
            }
        }

        `when`("quoted") {
            val result = quoted(input)

            then("it should return the string wrapped in quotes") {
                result shouldBe "\"$input\""
            }
        }
    }

    given("a string with a colon") {
        val input = "key:value"

        `when`("encoded") {
            val result = encoded(input)

            then("it should escape the colon") {
                result shouldBe "key\\:value"
            }
        }

        `when`("quoted") {
            val result = quoted(input)

            then("it should return quoted and escaped") {
                result shouldBe "\"key\\:value\""
            }
        }
    }

    given("a string with multiple Solr special characters") {
        val input = "+hello:(world)?"
        val expectedEncoded = "\\+hello\\:\\(world\\)\\?"

        `when`("encoded") {
            val result = encoded(input)

            then("it should escape all special characters") {
                result shouldBe expectedEncoded
            }
        }

        `when`("quoted") {
            val result = quoted(input)

            then("it should return quoted and escaped string") {
                result shouldBe "\"$expectedEncoded\""
            }
        }
    }

    given("an empty string") {
        val input = ""

        `when`("encoded") {
            val result = encoded(input)

            then("it should return an empty string") {
                result shouldBe ""
            }
        }

        `when`("quoted") {
            val result = quoted(input)

            then("it should return a pair of quotes") {
                result shouldBe "\"\""
            }
        }
    }
})
