/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.history.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import nl.info.client.zgw.drc.model.generated.StatusEnum
import nl.info.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum
import nl.info.zac.history.model.HistoryLine
import java.time.LocalDate
import java.time.ZonedDateTime

class AuditWijzigingAttributeHelpersTest : BehaviorSpec({

    context("String overload of addHistorieRegel") {
        given("Two different string values") {
            val list = mutableListOf<HistoryLine>()

            `when`("addHistorieRegel is called") {
                list.addHistorieRegel("fakeLabel", "fakeOud", "fakeNieuw")

                then("a HistoryLine is added") {
                    list.size shouldBe 1
                    list[0].attributeLabel shouldBe "fakeLabel"
                }
            }
        }

        given("Two equal string values") {
            val list = mutableListOf<HistoryLine>()

            `when`("addHistorieRegel is called") {
                list.addHistorieRegel("fakeLabel", "fakeSame", "fakeSame")

                then("no HistoryLine is added") {
                    list.shouldBeEmpty()
                }
            }
        }
    }

    context("Boolean overload of addHistorieRegel") {
        given("Different boolean values (false, true)") {
            val list = mutableListOf<HistoryLine>()

            `when`("addHistorieRegel is called") {
                list.addHistorieRegel("fakeLabel", false, true)

                then("a HistoryLine is added") {
                    list.size shouldBe 1
                }
            }
        }

        given("Equal boolean values (true, true)") {
            val list = mutableListOf<HistoryLine>()

            `when`("addHistorieRegel is called") {
                list.addHistorieRegel("fakeLabel", true, true)

                then("no HistoryLine is added") {
                    list.shouldBeEmpty()
                }
            }
        }
    }

    context("Nullable LocalDate overload of addHistorieRegel") {
        given("Two different non-null LocalDate values") {
            val list = mutableListOf<HistoryLine>()

            `when`("addHistorieRegel is called") {
                list.addHistorieRegel("fakeLabel", LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1))

                then("a HistoryLine is added") {
                    list.size shouldBe 1
                }
            }
        }

        given("One non-null and one null LocalDate") {
            val list = mutableListOf<HistoryLine>()

            `when`("addHistorieRegel is called with old non-null and new null") {
                list.addHistorieRegel("fakeLabel", LocalDate.of(2024, 1, 1), null)

                then("a HistoryLine is added") {
                    list.size shouldBe 1
                }
            }
        }

        given("Two null LocalDate values") {
            val list = mutableListOf<HistoryLine>()

            `when`("addHistorieRegel is called with both null") {
                list.addHistorieRegel("fakeLabel", null as LocalDate?, null as LocalDate?)

                then("no HistoryLine is added") {
                    list.shouldBeEmpty()
                }
            }
        }
    }

    context("ZonedDateTime overload of addHistorieRegel") {
        given("Two different ZonedDateTime values") {
            val list = mutableListOf<HistoryLine>()
            val oudDateTime = ZonedDateTime.now().minusDays(1)
            val nieuwDateTime = ZonedDateTime.now()

            `when`("addHistorieRegel is called") {
                list.addHistorieRegel("fakeLabel", oudDateTime, nieuwDateTime)

                then("a HistoryLine is added") {
                    list.size shouldBe 1
                }
            }
        }
    }

    context("StatusEnum overload of addHistorieRegel") {
        given("Two different StatusEnum values") {
            val list = mutableListOf<HistoryLine>()

            `when`("addHistorieRegel is called") {
                list.addHistorieRegel("fakeLabel", StatusEnum.IN_BEWERKING, StatusEnum.DEFINITIEF)

                then("a HistoryLine is added") {
                    list.size shouldBe 1
                }
            }
        }

        given("Two equal StatusEnum values") {
            val list = mutableListOf<HistoryLine>()

            `when`("addHistorieRegel is called") {
                list.addHistorieRegel("fakeLabel", StatusEnum.DEFINITIEF, StatusEnum.DEFINITIEF)

                then("no HistoryLine is added") {
                    list.shouldBeEmpty()
                }
            }
        }
    }

    context("VertrouwelijkheidaanduidingEnum overload of addHistorieRegel") {
        given("Two different VertrouwelijkheidaanduidingEnum values") {
            val list = mutableListOf<HistoryLine>()

            `when`("addHistorieRegel is called") {
                list.addHistorieRegel(
                    "fakeLabel",
                    VertrouwelijkheidaanduidingEnum.OPENBAAR,
                    VertrouwelijkheidaanduidingEnum.CONFIDENTIEEL
                )

                then("a HistoryLine is added") {
                    list.size shouldBe 1
                }
            }
        }

        given("Two equal VertrouwelijkheidaanduidingEnum values") {
            val list = mutableListOf<HistoryLine>()

            `when`("addHistorieRegel is called") {
                list.addHistorieRegel(
                    "fakeLabel",
                    VertrouwelijkheidaanduidingEnum.OPENBAAR,
                    VertrouwelijkheidaanduidingEnum.OPENBAAR
                )

                then("no HistoryLine is added") {
                    list.shouldBeEmpty()
                }
            }
        }
    }
})
