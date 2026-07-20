/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util.time

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.Period

class PeriodUtilTest : BehaviorSpec({

    given("a zero period") {
        `when`("formatting it") {
            val result = PeriodUtil.format(Period.ZERO)

            then("it should return '0 dagen'") {
                result shouldBe "0 dagen"
            }
        }
    }

    given("a period of one year, one month and one day") {
        val period = Period.of(1, 1, 1)

        `when`("formatting it") {
            val result = PeriodUtil.format(period)

            then("it should use the singular forms joined by comma") {
                result shouldBe "1 jaar, 1 maand, 1 dag"
            }
        }
    }

    given("a period of two years, two months and two days") {
        val period = Period.of(2, 2, 2)

        `when`("formatting it") {
            val result = PeriodUtil.format(period)

            then("it should use the plural forms joined by comma") {
                result shouldBe "2 jaren, 2 maanden, 2 dagen"
            }
        }
    }

    given("a period of only days") {
        val period = Period.ofDays(5)

        `when`("formatting it") {
            val result = PeriodUtil.format(period)

            then("it should only contain the days part") {
                result shouldBe "5 dagen"
            }
        }

        `when`("calculating the number of days from today") {
            val result = PeriodUtil.numberOfDaysFromToday(period)

            then("it should return five") {
                result shouldBe 5
            }
        }
    }
})
