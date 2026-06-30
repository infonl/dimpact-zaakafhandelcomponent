/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import net.atos.zac.app.admin.model.RestHumanTaskReferenceTable
import net.atos.zac.app.admin.model.createRESTPlanItemDefinition
import nl.info.zac.admin.model.createZaaktypeCmmnHumantaskParameters
import nl.info.zac.app.planitems.model.PlanItemType

class RESTHumanTaskParametersConverterTest : BehaviorSpec({
    val restHumanTaskReferenceTableConverter = mockk<RestHumanTaskReferenceTableConverter>()
    val converter = RESTHumanTaskParametersConverter(restHumanTaskReferenceTableConverter)

    afterEach { checkUnnecessaryStub() }

    Context("convertHumanTaskParametersCollection") {
        Given("A plan item definition that matches an existing ZaaktypeCmmnHumantaskParameters") {
            val fakePlanItemDefinition = createRESTPlanItemDefinition(id = "AANVULLENDE_INFORMATIE")
            val fakeHumantaskParameters = createZaaktypeCmmnHumantaskParameters(
                planItemDefinitionId = "AANVULLENDE_INFORMATIE",
                id = 42L
            )
            val fakeReferentieTabellen = emptyList<RestHumanTaskReferenceTable>()

            every {
                restHumanTaskReferenceTableConverter.convert(fakeHumantaskParameters.getReferentieTabellen())
            } returns fakeReferentieTabellen

            When("convertHumanTaskParametersCollection is called") {
                val result = converter.convertHumanTaskParametersCollection(
                    listOf(fakeHumantaskParameters),
                    listOf(fakePlanItemDefinition)
                )

                Then("the result contains one RESTHumanTaskParameters with populated fields") {
                    result.size shouldBe 1
                    result[0].id shouldBe 42L
                    result[0].actief shouldBe true
                    result[0].defaultGroepId shouldBe "fakeGroepId"
                    result[0].doorlooptijd shouldBe 5
                    result[0].planItemDefinition shouldBe fakePlanItemDefinition
                }
            }
        }

        Given("A plan item definition with no matching ZaaktypeCmmnHumantaskParameters") {
            val fakePlanItemDefinition = createRESTPlanItemDefinition(id = "AANVULLENDE_INFORMATIE")

            When("convertHumanTaskParametersCollection is called with empty parameters collection") {
                val result = converter.convertHumanTaskParametersCollection(
                    emptyList(),
                    listOf(fakePlanItemDefinition)
                )

                Then("the result contains one RESTHumanTaskParameters with default values") {
                    result.size shouldBe 1
                    result[0].actief shouldBe false
                    result[0].planItemDefinition shouldBe fakePlanItemDefinition
                    result[0].id shouldBe null
                    result[0].formulierDefinitieId shouldBe "AANVULLENDE_INFORMATIE"
                }
            }
        }
    }

    Context("convertRESTHumanTaskParameters") {
        Given("A list with one RESTHumanTaskParameters") {
            val fakePlanItemDefinition = createRESTPlanItemDefinition(id = "AANVULLENDE_INFORMATIE")
            val fakeReferentieTabellen = emptyList<RestHumanTaskReferenceTable>()
            val restParams = net.atos.zac.app.admin.model.RESTHumanTaskParameters().apply {
                id = 10L
                actief = true
                planItemDefinition = fakePlanItemDefinition
                defaultGroepId = "fakeGroepId"
                formulierDefinitieId = "AANVULLENDE_INFORMATIE"
                doorlooptijd = 3
                referentieTabellen = fakeReferentieTabellen.toMutableList()
            }

            every {
                restHumanTaskReferenceTableConverter.convert(fakeReferentieTabellen)
            } returns emptyList()

            When("convertRESTHumanTaskParameters is called") {
                val result = converter.convertRESTHumanTaskParameters(listOf(restParams))

                Then("the result contains one ZaaktypeCmmnHumantaskParameters with all fields mapped") {
                    result.size shouldBe 1
                    result[0].id shouldBe 10L
                    result[0].actief shouldBe true
                    result[0].planItemDefinitionID shouldBe "AANVULLENDE_INFORMATIE"
                    result[0].groepID shouldBe "fakeGroepId"
                    result[0].doorlooptijd shouldBe 3
                }
            }
        }

        Given("A list with two RESTHumanTaskParameters") {
            val fakeReferentieTabellen = emptyList<RestHumanTaskReferenceTable>()
            val restParams1 = net.atos.zac.app.admin.model.RESTHumanTaskParameters().apply {
                planItemDefinition = createRESTPlanItemDefinition(id = "AANVULLENDE_INFORMATIE")
                referentieTabellen = fakeReferentieTabellen.toMutableList()
            }
            val restParams2 = net.atos.zac.app.admin.model.RESTHumanTaskParameters().apply {
                planItemDefinition = createRESTPlanItemDefinition(
                    id = "AANVULLENDE_INFORMATIE",
                    type = PlanItemType.HUMAN_TASK
                )
                referentieTabellen = fakeReferentieTabellen.toMutableList()
            }

            every {
                restHumanTaskReferenceTableConverter.convert(fakeReferentieTabellen)
            } returns emptyList()

            When("convertRESTHumanTaskParameters is called with two entries") {
                val result = converter.convertRESTHumanTaskParameters(listOf(restParams1, restParams2))

                Then("the result list has size 2") {
                    result.size shouldBe 2
                }
            }
        }
    }
})
