/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class AddressUtilsTest : BehaviorSpec({
    context("formatHuisnummer") {
        given("A house number with a huisletter and a huisnummertoevoeging") {
            `when`("the house number is formatted") {
                val huisnummer = formatHuisnummer(12, "A", "bis")

                then("the huisletter follows the number directly and the huisnummertoevoeging follows after a space") {
                    huisnummer shouldBe "12A bis"
                }
            }
        }

        given("A house number with only a huisnummertoevoeging") {
            `when`("the house number is formatted") {
                val huisnummer = formatHuisnummer(12, null, "bis")

                then("the huisnummertoevoeging follows the number after a space") {
                    huisnummer shouldBe "12 bis"
                }
            }
        }

        given("A house number with a blank huisletter and a blank huisnummertoevoeging") {
            `when`("the house number is formatted") {
                val huisnummer = formatHuisnummer(12, " ", " ")

                then("it is only the number, without extra spaces") {
                    huisnummer shouldBe "12"
                }
            }
        }

        given("No house number, huisletter and huisnummertoevoeging") {
            `when`("the house number is formatted") {
                val huisnummer = formatHuisnummer(null, null, null)

                then("it is empty") {
                    huisnummer shouldBe ""
                }
            }
        }
    }
})
