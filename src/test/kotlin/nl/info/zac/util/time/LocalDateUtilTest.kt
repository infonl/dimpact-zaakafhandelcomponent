/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util.time

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.client.zgw.ztc.model.createBesluitType
import java.time.LocalDate

class LocalDateUtilTest : BehaviorSpec({

    given("a date range with both begin and end set") {
        val begin = LocalDate.now().minusDays(1)
        val end = LocalDate.now().plusDays(1)

        `when`("now falls within the range") {
            val result = LocalDateUtil.dateNowIsBetween(begin, end)

            then("it should return true") {
                result shouldBe true
            }
        }

        `when`("now equals the begin date") {
            val result = LocalDateUtil.dateNowIsBetween(LocalDate.now(), end)

            then("it should return true") {
                result shouldBe true
            }
        }

        `when`("now equals the end date") {
            val result = LocalDateUtil.dateNowIsBetween(begin, LocalDate.now())

            then("it should return false") {
                result shouldBe false
            }
        }

        `when`("now is before the begin date") {
            val result = LocalDateUtil.dateNowIsBetween(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2))

            then("it should return false") {
                result shouldBe false
            }
        }

        `when`("now is after the end date") {
            val result = LocalDateUtil.dateNowIsBetween(LocalDate.now().minusDays(2), LocalDate.now().minusDays(1))

            then("it should return false") {
                result shouldBe false
            }
        }
    }

    given("a date range with a null begin date") {
        `when`("now is before the end date") {
            val result = LocalDateUtil.dateNowIsBetween(null, LocalDate.now().plusDays(1))

            then("it should return true") {
                result shouldBe true
            }
        }
    }

    given("a date range with a null end date") {
        `when`("now is after the begin date") {
            val result = LocalDateUtil.dateNowIsBetween(LocalDate.now().minusDays(1), null)

            then("it should return true") {
                result shouldBe true
            }
        }
    }

    given("a besluittype that is currently valid") {
        val besluitType = createBesluitType().apply {
            beginGeldigheid = LocalDate.now().minusDays(1)
            eindeGeldigheid = LocalDate.now().plusDays(1)
        }

        `when`("checking whether now falls within its validity period") {
            val result = LocalDateUtil.dateNowIsBetween(besluitType)

            then("it should return true") {
                result shouldBe true
            }
        }
    }

    given("a date string in ISO format") {
        val date = "2024-03-11"

        `when`("formatting the date") {
            val formatted = LocalDateUtil.format(date)

            then("it should be formatted as dd-MM-yyyy") {
                formatted shouldBe "11-03-2024"
            }
        }
    }

    given("a null date string") {
        `when`("formatting the date") {
            val formatted = LocalDateUtil.format(null)

            then("it should return null") {
                formatted shouldBe null
            }
        }
    }
})
