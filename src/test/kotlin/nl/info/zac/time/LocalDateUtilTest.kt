/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.time

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.client.zgw.ztc.model.createBesluitType
import java.time.LocalDate
import java.time.format.DateTimeParseException

class LocalDateUtilTest : BehaviorSpec({

    given("a date range with both begin and end set") {
        val begin = ZacTime.today().minusDays(1)
        val end = ZacTime.today().plusDays(1)

        `when`("now falls within the range") {
            val isDateNowBetween = dateNowIsBetween(begin, end)

            then("it should return true") {
                isDateNowBetween shouldBe true
            }
        }

        `when`("now equals the begin date") {
            val isDateNowBetween = dateNowIsBetween(ZacTime.today(), end)

            then("it should return true") {
                isDateNowBetween shouldBe true
            }
        }

        `when`("now equals the end date") {
            val isDateNowBetween = dateNowIsBetween(begin, ZacTime.today())

            then("it should return false") {
                isDateNowBetween shouldBe false
            }
        }

        `when`("now is before the begin date") {
            val isDateNowBetween = dateNowIsBetween(ZacTime.today().plusDays(1), ZacTime.today().plusDays(2))

            then("it should return false") {
                isDateNowBetween shouldBe false
            }
        }

        `when`("now is after the end date") {
            val isDateNowBetween = dateNowIsBetween(ZacTime.today().minusDays(2), ZacTime.today().minusDays(1))

            then("it should return false") {
                isDateNowBetween shouldBe false
            }
        }
    }

    given("a date range with a null begin date") {
        `when`("now is before the end date") {
            val isDateNowBetween = dateNowIsBetween(null, ZacTime.today().plusDays(1))

            then("it should return true") {
                isDateNowBetween shouldBe true
            }
        }
    }

    given("a date range with a null end date") {
        `when`("now is after the begin date") {
            val isDateNowBetween = dateNowIsBetween(ZacTime.today().minusDays(1), null)

            then("it should return true") {
                isDateNowBetween shouldBe true
            }
        }
    }

    given("a besluittype that is currently valid") {
        val besluitType = createBesluitType().apply {
            beginGeldigheid = ZacTime.today().minusDays(1)
            eindeGeldigheid = ZacTime.today().plusDays(1)
        }

        `when`("checking whether now falls within its validity period") {
            val isDateNowBetween = dateNowIsBetween(besluitType)

            then("it should return true") {
                isDateNowBetween shouldBe true
            }
        }
    }

    given("a besluittype without a beginGeldigheid") {
        val besluitType = createBesluitType().apply {
            eindeGeldigheid = ZacTime.today().plusDays(1)
        }

        `when`("checking whether now falls within its validity period") {
            val isDateNowBetween = dateNowIsBetween(besluitType)

            then("it should return true") {
                isDateNowBetween shouldBe true
            }
        }
    }

    given("a date string in ISO format") {
        val date = "2024-03-11"

        `when`("formatting the date") {
            val formatted = date.toFormattedDate()

            then("it should be formatted as dd-MM-yyyy") {
                formatted shouldBe "11-03-2024"
            }
        }
    }

    context("Reading a date that the frontend sent as a string") {
        given("a date without a time, as the frontend sends a picked date") {
            `when`("reading it as a date") {
                val localDate = "2026-01-30".toLocalDate()

                then("it is that date") {
                    localDate shouldBe LocalDate.of(2026, 1, 30)
                }
            }
        }

        given("midnight in Europe/Amsterdam with its offset, as an older frontend sent a picked date") {
            `when`("reading it as a date") {
                val localDate = "2026-01-30T00:00:00.000+01:00".toLocalDate()

                then("it is that date") {
                    localDate shouldBe LocalDate.of(2026, 1, 30)
                }
            }
        }

        given("the UTC instant of midnight in Europe/Amsterdam") {
            `when`("reading it as a date") {
                val localDate = "2026-07-29T22:00:00.000Z".toLocalDate()

                then("it is the date of that instant in Europe/Amsterdam") {
                    localDate shouldBe LocalDate.of(2026, 7, 30)
                }
            }
        }

        given("midnight in a browser time zone ahead of Europe/Amsterdam") {
            `when`("reading it as a date") {
                val localDate = "2026-01-30T00:00:00.000+02:00".toLocalDate()

                then("it is the date of that instant in Europe/Amsterdam, the day before") {
                    localDate shouldBe LocalDate.of(2026, 1, 29)
                }
            }
        }

        given("a string that is not a date") {
            `when`("reading it as a date") {
                val dateTimeParseException = shouldThrow<DateTimeParseException> {
                    "not a date".toLocalDate()
                }

                then("it is rejected") {
                    dateTimeParseException.parsedString shouldBe "not a date"
                }
            }
        }
    }
})
