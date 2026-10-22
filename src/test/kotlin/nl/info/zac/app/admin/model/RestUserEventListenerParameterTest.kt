/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.admin.model.ZaaktypeCmmnUsereventlistenerParameters
import nl.info.zac.app.planitems.model.PlanItemType

class RestUserEventListenerParameterTest : BehaviorSpec({
    context("toRestUserEventListenerParameter") {
        given("A user event listener definition with stored parameters for the same plan item definition") {
            val restPlanItemDefinition = createRestPlanItemDefinition(
                id = "fakeUserEventListenerId",
                naam = "fakeUserEventListenerNaam",
                type = PlanItemType.USER_EVENT_LISTENER
            )
            val zaaktypeCmmnUsereventlistenerParametersCollection = listOf(
                ZaaktypeCmmnUsereventlistenerParameters().apply {
                    planItemDefinitionID = "fakeOtherUserEventListenerId"
                    toelichting = "fakeOtherToelichting"
                },
                ZaaktypeCmmnUsereventlistenerParameters().apply {
                    planItemDefinitionID = "fakeUserEventListenerId"
                    toelichting = "fakeToelichting"
                }
            )

            `when`("it is converted to a REST user event listener parameter") {
                val restUserEventListenerParameter = restPlanItemDefinition.toRestUserEventListenerParameter(
                    zaaktypeCmmnUsereventlistenerParametersCollection
                )

                then("it has the id and naam of the definition and the toelichting of the matching parameters") {
                    with(restUserEventListenerParameter) {
                        id shouldBe "fakeUserEventListenerId"
                        naam shouldBe "fakeUserEventListenerNaam"
                        toelichting shouldBe "fakeToelichting"
                    }
                }
            }
        }

        given("A user event listener definition without stored parameters") {
            val restPlanItemDefinition = createRestPlanItemDefinition(
                id = "fakeUserEventListenerId",
                type = PlanItemType.USER_EVENT_LISTENER
            )

            `when`("it is converted to a REST user event listener parameter") {
                val restUserEventListenerParameter = restPlanItemDefinition.toRestUserEventListenerParameter(emptyList())

                then("it has no toelichting") {
                    restUserEventListenerParameter.id shouldBe "fakeUserEventListenerId"
                    restUserEventListenerParameter.toelichting shouldBe null
                }
            }
        }
    }

    context("toZaaktypeCmmnUsereventlistenerParameters") {
        given("A REST user event listener parameter with a toelichting") {
            val restUserEventListenerParameter = RestUserEventListenerParameter(
                id = "fakeUserEventListenerId",
                naam = "fakeUserEventListenerNaam",
                toelichting = "fakeToelichting"
            )

            `when`("it is converted to zaaktype CMMN user event listener parameters") {
                val zaaktypeCmmnUsereventlistenerParameters =
                    restUserEventListenerParameter.toZaaktypeCmmnUsereventlistenerParameters()

                then("the plan item definition id and toelichting are copied") {
                    zaaktypeCmmnUsereventlistenerParameters.planItemDefinitionID shouldBe "fakeUserEventListenerId"
                    zaaktypeCmmnUsereventlistenerParameters.toelichting shouldBe "fakeToelichting"
                }
            }
        }
    }
})
