/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.gebruikersvoorkeuren.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import net.atos.zac.gebruikersvoorkeuren.model.DashboardCardId
import net.atos.zac.gebruikersvoorkeuren.model.createDashboardCardInstelling
import net.atos.zac.signalering.model.SignaleringType
import net.atos.zac.app.gebruikersvoorkeuren.model.RESTDashboardCardInstelling

class RESTDashboardCardInstellingConverterTest : BehaviorSpec({
    Context("convert(DashboardCardInstelling)") {
        Given("a DashboardCardInstelling domain model") {
            val card = createDashboardCardInstelling(
                id = 7L,
                cardId = DashboardCardId.MIJN_TAKEN,
                kolom = 2,
                volgorde = 3
            )

            When("convert(DashboardCardInstelling) is called") {
                val result = RESTDashboardCardInstellingConverter.convert(card)

                Then("it maps all fields correctly") {
                    result.id shouldBe 7L
                    result.cardId shouldBe DashboardCardId.MIJN_TAKEN
                    result.column shouldBe 2
                    result.row shouldBe 3
                }
            }
        }
    }

    Context("convert(RESTDashboardCardInstelling)") {
        Given("a RESTDashboardCardInstelling") {
            val restCard = RESTDashboardCardInstelling().apply {
                id = 5L
                cardId = DashboardCardId.MIJN_TAKEN
                signaleringType = SignaleringType.Type.ZAAK_OP_NAAM
                column = 1
                row = 0
            }

            When("convert(RESTDashboardCardInstelling) is called") {
                val result = RESTDashboardCardInstellingConverter.convert(restCard)

                Then("it maps all fields correctly including signaleringType") {
                    result.id shouldBe 5L
                    result.cardId shouldBe DashboardCardId.MIJN_TAKEN
                    result.signaleringType shouldBe SignaleringType.Type.ZAAK_OP_NAAM
                    result.kolom shouldBe 1
                    result.volgorde shouldBe 0
                }
            }
        }
    }

    Context("convert(List<DashboardCardInstelling>)") {
        Given("a list of DashboardCardInstelling domain models") {
            val cards = listOf(
                createDashboardCardInstelling(id = 1L, cardId = DashboardCardId.MIJN_TAKEN, kolom = 0, volgorde = 0),
                createDashboardCardInstelling(id = 2L, cardId = DashboardCardId.MIJN_TAKEN, kolom = 1, volgorde = 1)
            )

            When("convert(List<DashboardCardInstelling>) is called") {
                val result = RESTDashboardCardInstellingConverter.convert(cards)

                Then("it returns a list of REST models with correct size") {
                    result.size shouldBe 2
                    result[0].id shouldBe 1L
                    result[1].id shouldBe 2L
                }
            }
        }
    }
})
