/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.gebruikersvoorkeuren.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.gebruikersvoorkeuren.model.DashboardCardId
import nl.info.zac.gebruikersvoorkeuren.model.createDashboardCardInstelling
import nl.info.zac.signalering.model.SignaleringType

class RestDashboardCardInstellingTest : BehaviorSpec({
    context("toRestDashboardCardInstelling") {
        given("a DashboardCardInstelling domain model") {
            val dashboardCardInstelling = createDashboardCardInstelling(
                id = 7L,
                cardId = DashboardCardId.MIJN_TAKEN,
                kolom = 2,
                volgorde = 3
            )

            `when`("it is converted to a REST model") {
                val restDashboardCardInstelling = dashboardCardInstelling.toRestDashboardCardInstelling()

                then("it maps all fields correctly") {
                    with(restDashboardCardInstelling) {
                        id shouldBe 7L
                        cardId shouldBe DashboardCardId.MIJN_TAKEN
                        column shouldBe 2
                        row shouldBe 3
                    }
                }
            }
        }
    }

    context("toDashboardCardInstelling") {
        given("a RestDashboardCardInstelling") {
            val restDashboardCardInstelling = RestDashboardCardInstelling(
                id = 5L,
                cardId = DashboardCardId.MIJN_TAKEN,
                signaleringType = SignaleringType.Type.ZAAK_OP_NAAM,
                column = 1,
                row = 0
            )

            `when`("it is converted to a domain model") {
                val dashboardCardInstelling = restDashboardCardInstelling.toDashboardCardInstelling()

                then("it maps all fields correctly including signaleringType") {
                    with(dashboardCardInstelling) {
                        id shouldBe 5L
                        cardId shouldBe DashboardCardId.MIJN_TAKEN
                        signaleringType shouldBe SignaleringType.Type.ZAAK_OP_NAAM
                        kolom shouldBe 1
                        volgorde shouldBe 0
                    }
                }
            }
        }
    }
})
