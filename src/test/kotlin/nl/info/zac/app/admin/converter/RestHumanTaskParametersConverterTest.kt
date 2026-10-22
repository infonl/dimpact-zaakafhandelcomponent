/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.zac.admin.model.createZaaktypeCmmnHumantaskParameters
import nl.info.zac.app.admin.model.RestHumanTaskParameters
import nl.info.zac.app.admin.model.RestHumanTaskReferenceTable
import nl.info.zac.app.admin.model.createRestPlanItemDefinition
import nl.info.zac.app.planitems.model.PlanItemType

class RestHumanTaskParametersConverterTest : BehaviorSpec({
    val restHumanTaskReferenceTableConverter = mockk<RestHumanTaskReferenceTableConverter>()
    val converter = RestHumanTaskParametersConverter(restHumanTaskReferenceTableConverter)

    afterEach { checkUnnecessaryStub() }

    context("convertHumanTaskParametersCollection") {
        given("A plan item definition that matches an existing ZaaktypeCmmnHumantaskParameters") {
            val fakePlanItemDefinition = createRestPlanItemDefinition(id = "AANVULLENDE_INFORMATIE")
            val fakeHumantaskParameters = createZaaktypeCmmnHumantaskParameters(
                planItemDefinitionId = "AANVULLENDE_INFORMATIE",
                id = 42L
            )
            val fakeReferentieTabellen = emptyList<RestHumanTaskReferenceTable>()

            every {
                restHumanTaskReferenceTableConverter.toRestHumanTaskReferenceTables(fakeHumantaskParameters.getReferentieTabellen())
            } returns fakeReferentieTabellen

            `when`("convertHumanTaskParametersCollection is called") {
                val result = converter.convertHumanTaskParametersCollection(
                    listOf(fakeHumantaskParameters),
                    listOf(fakePlanItemDefinition)
                )

                then("the result contains one RestHumanTaskParameters with populated fields") {
                    result.size shouldBe 1
                    result[0].id shouldBe 42L
                    result[0].isActief shouldBe true
                    result[0].defaultGroepId shouldBe "fakeGroepId"
                    result[0].doorlooptijd shouldBe 5
                    result[0].planItemDefinition shouldBe fakePlanItemDefinition
                }
            }
        }

        given("A plan item definition with no matching ZaaktypeCmmnHumantaskParameters") {
            val fakePlanItemDefinition = createRestPlanItemDefinition(id = "AANVULLENDE_INFORMATIE")

            `when`("convertHumanTaskParametersCollection is called with empty parameters collection") {
                val result = converter.convertHumanTaskParametersCollection(
                    emptyList(),
                    listOf(fakePlanItemDefinition)
                )

                then("the result contains one RestHumanTaskParameters with default values") {
                    result.size shouldBe 1
                    result[0].isActief shouldBe false
                    result[0].planItemDefinition shouldBe fakePlanItemDefinition
                    result[0].id shouldBe null
                    result[0].formulierDefinitieId shouldBe "AANVULLENDE_INFORMATIE"
                }
            }
        }
    }

    context("convertRestHumanTaskParameters") {
        given("A list with one RestHumanTaskParameters") {
            val fakePlanItemDefinition = createRestPlanItemDefinition(id = "AANVULLENDE_INFORMATIE")
            val fakeReferentieTabellen = emptyList<RestHumanTaskReferenceTable>()
            val restHumanTaskParameters = RestHumanTaskParameters(
                id = 10L,
                isActief = true,
                planItemDefinition = fakePlanItemDefinition,
                defaultGroepId = "fakeGroepId",
                formulierDefinitieId = "AANVULLENDE_INFORMATIE",
                doorlooptijd = 3,
                referentieTabellen = fakeReferentieTabellen
            )

            every {
                restHumanTaskReferenceTableConverter.toHumanTaskReferentieTabellen(fakeReferentieTabellen)
            } returns emptyList()

            `when`("convertRestHumanTaskParameters is called") {
                val result = converter.convertRestHumanTaskParameters(listOf(restHumanTaskParameters))

                then("the result contains one ZaaktypeCmmnHumantaskParameters with all fields mapped") {
                    result.size shouldBe 1
                    result[0].id shouldBe 10L
                    result[0].isActief shouldBe true
                    result[0].planItemDefinitionID shouldBe "AANVULLENDE_INFORMATIE"
                    result[0].groepID shouldBe "fakeGroepId"
                    result[0].doorlooptijd shouldBe 3
                }
            }
        }

        given("A list with two RestHumanTaskParameters") {
            val fakeReferentieTabellen = emptyList<RestHumanTaskReferenceTable>()
            val restHumanTaskParameters1 = RestHumanTaskParameters(
                planItemDefinition = createRestPlanItemDefinition(id = "AANVULLENDE_INFORMATIE"),
                referentieTabellen = fakeReferentieTabellen
            )
            val restHumanTaskParameters2 = RestHumanTaskParameters(
                planItemDefinition = createRestPlanItemDefinition(
                    id = "AANVULLENDE_INFORMATIE",
                    type = PlanItemType.HUMAN_TASK
                ),
                referentieTabellen = fakeReferentieTabellen
            )

            every {
                restHumanTaskReferenceTableConverter.toHumanTaskReferentieTabellen(fakeReferentieTabellen)
            } returns emptyList()

            `when`("convertRestHumanTaskParameters is called with two entries") {
                val result = converter.convertRestHumanTaskParameters(listOf(restHumanTaskParameters1, restHumanTaskParameters2))

                then("the result list has size 2") {
                    result.size shouldBe 2
                }
            }
        }
    }
})
