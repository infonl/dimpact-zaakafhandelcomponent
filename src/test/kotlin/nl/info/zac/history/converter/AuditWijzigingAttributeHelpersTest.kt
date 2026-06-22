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

    Context("String overload of addHistorieRegel") {
        Given("Two different string values") {
            val list = mutableListOf<HistoryLine>()

            When("addHistorieRegel is called") {
                list.addHistorieRegel("fakeLabel", "fakeOud", "fakeNieuw")

                Then("a HistoryLine is added") {
                    list.size shouldBe 1
                    list[0].attribuutLabel shouldBe "fakeLabel"
                }
            }
        }

        Given("Two equal string values") {
            val list = mutableListOf<HistoryLine>()

            When("addHistorieRegel is called") {
                list.addHistorieRegel("fakeLabel", "fakeSame", "fakeSame")

                Then("no HistoryLine is added") {
                    list.shouldBeEmpty()
                }
            }
        }
    }

    Context("Boolean overload of addHistorieRegel") {
        Given("Different boolean values (false, true)") {
            val list = mutableListOf<HistoryLine>()

            When("addHistorieRegel is called") {
                list.addHistorieRegel("fakeLabel", false, true)

                Then("a HistoryLine is added") {
                    list.size shouldBe 1
                }
            }
        }

        Given("Equal boolean values (true, true)") {
            val list = mutableListOf<HistoryLine>()

            When("addHistorieRegel is called") {
                list.addHistorieRegel("fakeLabel", true, true)

                Then("no HistoryLine is added") {
                    list.shouldBeEmpty()
                }
            }
        }
    }

    Context("Nullable LocalDate overload of addHistorieRegel") {
        Given("Two different non-null LocalDate values") {
            val list = mutableListOf<HistoryLine>()

            When("addHistorieRegel is called") {
                list.addHistorieRegel("fakeLabel", LocalDate.of(2024, 1, 1), LocalDate.of(2025, 1, 1))

                Then("a HistoryLine is added") {
                    list.size shouldBe 1
                }
            }
        }

        Given("One non-null and one null LocalDate") {
            val list = mutableListOf<HistoryLine>()

            When("addHistorieRegel is called with old non-null and new null") {
                list.addHistorieRegel("fakeLabel", LocalDate.of(2024, 1, 1), null)

                Then("a HistoryLine is added") {
                    list.size shouldBe 1
                }
            }
        }

        Given("Two null LocalDate values") {
            val list = mutableListOf<HistoryLine>()

            When("addHistorieRegel is called with both null") {
                list.addHistorieRegel("fakeLabel", null as LocalDate?, null as LocalDate?)

                Then("no HistoryLine is added") {
                    list.shouldBeEmpty()
                }
            }
        }
    }

    Context("ZonedDateTime overload of addHistorieRegel") {
        Given("Two different ZonedDateTime values") {
            val list = mutableListOf<HistoryLine>()
            val oudDateTime = ZonedDateTime.now().minusDays(1)
            val nieuwDateTime = ZonedDateTime.now()

            When("addHistorieRegel is called") {
                list.addHistorieRegel("fakeLabel", oudDateTime, nieuwDateTime)

                Then("a HistoryLine is added") {
                    list.size shouldBe 1
                }
            }
        }
    }

    Context("StatusEnum overload of addHistorieRegel") {
        Given("Two different StatusEnum values") {
            val list = mutableListOf<HistoryLine>()

            When("addHistorieRegel is called") {
                list.addHistorieRegel("fakeLabel", StatusEnum.IN_BEWERKING, StatusEnum.DEFINITIEF)

                Then("a HistoryLine is added") {
                    list.size shouldBe 1
                }
            }
        }

        Given("Two equal StatusEnum values") {
            val list = mutableListOf<HistoryLine>()

            When("addHistorieRegel is called") {
                list.addHistorieRegel("fakeLabel", StatusEnum.DEFINITIEF, StatusEnum.DEFINITIEF)

                Then("no HistoryLine is added") {
                    list.shouldBeEmpty()
                }
            }
        }
    }

    Context("VertrouwelijkheidaanduidingEnum overload of addHistorieRegel") {
        Given("Two different VertrouwelijkheidaanduidingEnum values") {
            val list = mutableListOf<HistoryLine>()

            When("addHistorieRegel is called") {
                list.addHistorieRegel(
                    "fakeLabel",
                    VertrouwelijkheidaanduidingEnum.OPENBAAR,
                    VertrouwelijkheidaanduidingEnum.CONFIDENTIEEL
                )

                Then("a HistoryLine is added") {
                    list.size shouldBe 1
                }
            }
        }

        Given("Two equal VertrouwelijkheidaanduidingEnum values") {
            val list = mutableListOf<HistoryLine>()

            When("addHistorieRegel is called") {
                list.addHistorieRegel(
                    "fakeLabel",
                    VertrouwelijkheidaanduidingEnum.OPENBAAR,
                    VertrouwelijkheidaanduidingEnum.OPENBAAR
                )

                Then("no HistoryLine is added") {
                    list.shouldBeEmpty()
                }
            }
        }
    }
})
