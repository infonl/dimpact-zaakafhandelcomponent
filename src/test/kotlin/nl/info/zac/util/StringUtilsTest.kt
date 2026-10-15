/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class StringUtilsTest : BehaviorSpec({
    context("joinNonBlank") {
        given("A list with non-blank, blank, empty and null strings") {
            val parts = listOf("Kerkstraat", null, " ", "", "12")

            `when`("the parts are joined with a separator") {
                val joined = parts.joinNonBlank(" ")

                then("only the non-blank parts are joined, so no separator is doubled") {
                    joined shouldBe "Kerkstraat 12"
                }
            }

            `when`("the parts are joined without a separator") {
                val joined = parts.joinNonBlank()

                then("the non-blank parts are concatenated") {
                    joined shouldBe "Kerkstraat12"
                }
            }
        }

        given("A list with only blank and null strings") {
            val parts = listOf(null, " ", "")

            `when`("the parts are joined") {
                val joined = parts.joinNonBlank(", ")

                then("the result is empty") {
                    joined shouldBe ""
                }
            }
        }
    }

    context("isPureAscii") {
        given("A string with only ASCII characters") {
            `when`("it is checked for pure ASCII") {
                then("it is pure ASCII") {
                    "Kerkstraat 12".isPureAscii() shouldBe true
                }
            }
        }

        given("A string with a non-breaking space") {
            `when`("it is checked for pure ASCII") {
                then("it is not pure ASCII") {
                    "Kerkstraat${NON_BREAKING_SPACE}12".isPureAscii() shouldBe false
                }
            }
        }
    }
})
