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
import net.atos.zac.app.admin.model.RESTPlanItemDefinition
import net.atos.zac.app.admin.model.RestHumanTaskReferenceTable
import nl.info.zac.admin.model.ZaaktypeCmmnHumantaskParameters
import nl.info.zac.app.planitems.model.PlanItemType

class RESTHumanTaskParametersConverterTest : BehaviorSpec({
    val restHumanTaskReferenceTableConverter = mockk<RestHumanTaskReferenceTableConverter>()
    val converter = RESTHumanTaskParametersConverter(restHumanTaskReferenceTableConverter)

    afterEach { checkUnnecessaryStub() }

    fun createPlanItemDefinition(
        id: String = "AANVULLENDE_INFORMATIE",
        naam: String = "fakePlanItemNaam"
    ) = RESTPlanItemDefinition(id, naam, PlanItemType.HUMAN_TASK)

    fun createHumantaskParameters(
        planItemDefinitionId: String = "AANVULLENDE_INFORMATIE",
        id: Long = 1L,
        actief: Boolean = true,
        groepId: String = "fakeGroepId",
        doorlooptijd: Int = 5
    ) = ZaaktypeCmmnHumantaskParameters().apply {
        this.planItemDefinitionID = planItemDefinitionId
        this.id = id
        this.actief = actief
        this.groepID = groepId
        this.doorlooptijd = doorlooptijd
    }

    Context("convertHumanTaskParametersCollection with matching parameters") {
        Given("A plan item definition that matches an existing ZaaktypeCmmnHumantaskParameters") {
            val fakeDefinition = createPlanItemDefinition(id = "AANVULLENDE_INFORMATIE")
            val fakeParams = createHumantaskParameters(planItemDefinitionId = "AANVULLENDE_INFORMATIE", id = 42L)
            val fakeReferentieTabellen = emptyList<RestHumanTaskReferenceTable>()

            every {
                restHumanTaskReferenceTableConverter.convert(fakeParams.getReferentieTabellen())
            } returns fakeReferentieTabellen

            When("convertHumanTaskParametersCollection is called") {
                val result = converter.convertHumanTaskParametersCollection(
                    listOf(fakeParams),
                    listOf(fakeDefinition)
                )

                Then("the result contains one RESTHumanTaskParameters with populated fields") {
                    result.size shouldBe 1
                    result[0].id shouldBe 42L
                    result[0].actief shouldBe true
                    result[0].defaultGroepId shouldBe "fakeGroepId"
                    result[0].doorlooptijd shouldBe 5
                    result[0].planItemDefinition shouldBe fakeDefinition
                }
            }
        }
    }

    Context("convertHumanTaskParametersCollection with no matching parameters") {
        Given("A plan item definition with no matching ZaaktypeCmmnHumantaskParameters") {
            val fakeDefinition = createPlanItemDefinition(id = "AANVULLENDE_INFORMATIE")

            When("convertHumanTaskParametersCollection is called with empty parameters collection") {
                val result = converter.convertHumanTaskParametersCollection(
                    emptyList(),
                    listOf(fakeDefinition)
                )

                Then("the result contains one RESTHumanTaskParameters with default values") {
                    result.size shouldBe 1
                    result[0].actief shouldBe false
                    result[0].planItemDefinition shouldBe fakeDefinition
                    result[0].id shouldBe null
                    result[0].formulierDefinitieId shouldBe "AANVULLENDE_INFORMATIE"
                }
            }
        }
    }

    Context("convertRESTHumanTaskParameters with single entry") {
        Given("A list with one RESTHumanTaskParameters") {
            val fakeDefinition = createPlanItemDefinition(id = "AANVULLENDE_INFORMATIE")
            val fakeReferentieTabellen = emptyList<RestHumanTaskReferenceTable>()
            val restParams = net.atos.zac.app.admin.model.RESTHumanTaskParameters().apply {
                id = 10L
                actief = true
                planItemDefinition = fakeDefinition
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
    }

    Context("convertRESTHumanTaskParameters with multiple entries") {
        Given("A list with two RESTHumanTaskParameters") {
            val fakeReferentieTabellen = emptyList<RestHumanTaskReferenceTable>()
            val restParams1 = net.atos.zac.app.admin.model.RESTHumanTaskParameters().apply {
                planItemDefinition = createPlanItemDefinition(id = "AANVULLENDE_INFORMATIE")
                referentieTabellen = fakeReferentieTabellen.toMutableList()
            }
            val restParams2 = net.atos.zac.app.admin.model.RESTHumanTaskParameters().apply {
                planItemDefinition = createPlanItemDefinition(id = "AANVULLENDE_INFORMATIE")
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
