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
import io.mockk.slot
import jakarta.json.bind.JsonbBuilder
import nl.info.zac.admin.ResultaattypeReferenceService
import nl.info.zac.admin.ZaaktypeConfigurationBeheerService
import nl.info.zac.admin.model.ZaaktypeCmmnUsereventlistenerParameters
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.admin.model.createZaaktypeCmmnHumantaskParameters
import nl.info.zac.app.admin.model.RestHumanTaskParameters
import nl.info.zac.app.admin.model.RestPlanItemDefinition
import nl.info.zac.app.admin.model.RestUserEventListenerParameter
import nl.info.zac.app.admin.model.RestZaaktypeConfiguration
import nl.info.zac.app.admin.model.createRestProcessDefinition
import nl.info.zac.app.admin.model.createRestZaaktypeConfiguration
import nl.info.zac.app.admin.model.createRestZaaktypeOverzicht
import nl.info.zac.app.planitems.model.PlanItemType

class RestZaaktypeConfigurationConverterCaseDefinitionChangeTest : BehaviorSpec({
    val processDefinitionConverter = mockk<RestProcessDefinitionConverter>()
    val zaakbeeindigParameterConverter = mockk<RestZaakbeeindigParameterConverter>()
    val restHumanTaskParametersConverter = mockk<RestHumanTaskParametersConverter>()
    val zaaktypeConfigurationBeheerService = mockk<ZaaktypeConfigurationBeheerService>()
    val resultaattypeReferenceService = mockk<ResultaattypeReferenceService>()
    val restZaaktypeConfigurationConverter = RestZaaktypeConfigurationConverter(
        processDefinitionConverter = processDefinitionConverter,
        zaakbeeindigParameterConverter = zaakbeeindigParameterConverter,
        humanTaskParametersConverter = restHumanTaskParametersConverter,
        ztcClientService = mockk(),
        resultaattypeReferenceService = resultaattypeReferenceService,
        zaaktypeConfigurationBeheerService = zaaktypeConfigurationBeheerService,
        smartDocumentsService = mockk(),
        zaakspecifiekeAutorisatieService = mockk()
    )

    afterEach {
        checkUnnecessaryStub()
    }

    given("a stored CMMN configuration with the plan item settings of its case definition") {
        fun createStoredConfiguration() = createZaaktypeCmmnConfiguration(caseDefinitionId = "fakeCaseDefinitionA").apply {
            getOrCreateCmmnExtension().apply {
                setHumanTaskParametersCollection(
                    listOf(createZaaktypeCmmnHumantaskParameters(planItemDefinitionId = "fakeHumanTaskOfA"))
                )
                setUserEventListenerParametersCollection(
                    listOf(ZaaktypeCmmnUsereventlistenerParameters().apply { planItemDefinitionID = "fakeListenerOfA" })
                )
            }
        }
        val caseDefinitionB = createRestProcessDefinition(key = "fakeCaseDefinitionB").apply {
            humanTaskDefinitions = listOf(RestPlanItemDefinition("fakeHumanTaskOfB", null, PlanItemType.HUMAN_TASK))
            userEventListenerDefinitions = listOf(
                RestPlanItemDefinition("fakeListenerOfB", null, PlanItemType.USER_EVENT_LISTENER)
            )
        }

        `when`("it is saved with another case definition and no plan item settings") {
            val storedConfiguration = createStoredConfiguration()
            every {
                zaaktypeConfigurationBeheerService.findConfiguration(storedConfiguration.zaaktypeUuid)
            } returns storedConfiguration
            every { processDefinitionConverter.toRestProcessDefinition(any(), true) } returns caseDefinitionB
            every {
                with(restHumanTaskParametersConverter) { emptyList<RestHumanTaskParameters>().toZaaktypeCmmnHumantaskParameters() }
            } returns emptyList()
            val restZaaktypeConfiguration = JsonbBuilder.create().fromJson(
                """
                {
                  "zaaktype": { "uuid": "${storedConfiguration.zaaktypeUuid}", "omschrijving": "fakeOmschrijving" },
                  "processDefinition": { "processEngine": "CMMN", "key": "fakeCaseDefinitionB" },
                  "defaultGroepId": "fakeGroupId",
                  "smartDocuments": { "isEnabledGlobally": false, "isEnabledForZaaktype": false }
                }
                """.trimIndent(),
                RestZaaktypeConfiguration::class.java
            )

            val zaaktypeConfiguration = restZaaktypeConfigurationConverter.toZaaktypeConfiguration(
                restZaaktypeConfiguration
            )

            then("it holds no plan item settings of the previous case definition") {
                zaaktypeConfiguration.processBinding?.definitionKey shouldBe "fakeCaseDefinitionB"
                zaaktypeConfiguration.cmmnExtension?.getHumanTaskParametersCollection()?.size shouldBe 0
                zaaktypeConfiguration.cmmnExtension?.getUserEventListenerParametersCollection()?.size shouldBe 0
            }
        }

        `when`("it is saved with another case definition and the plan item settings of the previous one") {
            val storedConfiguration = createStoredConfiguration()
            every {
                zaaktypeConfigurationBeheerService.findConfiguration(storedConfiguration.zaaktypeUuid)
            } returns storedConfiguration
            every { processDefinitionConverter.toRestProcessDefinition(any(), true) } returns caseDefinitionB
            every { zaakbeeindigParameterConverter.toZaaktypeCompletionParameters(emptyList()) } returns emptyList()
            val humanTaskParametersSlot = slot<List<RestHumanTaskParameters>>()
            every {
                with(restHumanTaskParametersConverter) { capture(humanTaskParametersSlot).toZaaktypeCmmnHumantaskParameters() }
            } returns emptyList()
            val restZaaktypeConfiguration = createRestZaaktypeConfiguration(
                restZaaktypeOverzicht = createRestZaaktypeOverzicht(uuid = storedConfiguration.zaaktypeUuid),
                restProcessDefinition = createRestProcessDefinition(key = "fakeCaseDefinitionB")
            ).apply {
                humanTaskParameters = listOf(
                    RestHumanTaskParameters(
                        planItemDefinition = RestPlanItemDefinition("fakeHumanTaskOfA", null, PlanItemType.HUMAN_TASK)
                    )
                )
                userEventListenerParameters = listOf(RestUserEventListenerParameter(id = "fakeListenerOfA"))
            }

            val zaaktypeConfiguration = restZaaktypeConfigurationConverter.toZaaktypeConfiguration(
                restZaaktypeConfiguration
            )

            then("it stores only settings of plan items of the new case definition") {
                humanTaskParametersSlot.captured shouldBe emptyList()
                zaaktypeConfiguration.cmmnExtension?.getHumanTaskParametersCollection()?.size shouldBe 0
                zaaktypeConfiguration.cmmnExtension?.getUserEventListenerParametersCollection()?.size shouldBe 0
            }
        }

        `when`("it is saved with another case definition that shares a human task with the previous one") {
            val storedConfiguration = createStoredConfiguration().apply {
                cmmnExtension?.apply {
                    setHumanTaskParametersCollection(
                        listOf(
                            createZaaktypeCmmnHumantaskParameters(planItemDefinitionId = "fakeHumanTaskOfA"),
                            createZaaktypeCmmnHumantaskParameters(planItemDefinitionId = "fakeSharedTask")
                        )
                    )
                    findHumanTaskParameter("fakeHumanTaskOfA")?.id = 41L
                    findHumanTaskParameter("fakeSharedTask")?.id = 42L
                }
            }
            every {
                zaaktypeConfigurationBeheerService.findConfiguration(storedConfiguration.zaaktypeUuid)
            } returns storedConfiguration
            every { processDefinitionConverter.toRestProcessDefinition(any(), true) } returns
                createRestProcessDefinition(key = "fakeCaseDefinitionB").apply {
                    humanTaskDefinitions = listOf(
                        RestPlanItemDefinition("fakeSharedTask", null, PlanItemType.HUMAN_TASK),
                        RestPlanItemDefinition("fakeHumanTaskOfB", null, PlanItemType.HUMAN_TASK)
                    )
                }
            every { zaakbeeindigParameterConverter.toZaaktypeCompletionParameters(emptyList()) } returns emptyList()
            val sharedTaskParameters = RestHumanTaskParameters(
                planItemDefinition = RestPlanItemDefinition("fakeSharedTask", null, PlanItemType.HUMAN_TASK)
            )
            every {
                with(restHumanTaskParametersConverter) { listOf(sharedTaskParameters).toZaaktypeCmmnHumantaskParameters() }
            } returns listOf(
                createZaaktypeCmmnHumantaskParameters(planItemDefinitionId = "fakeSharedTask", id = 1L, doorlooptijd = 9)
            )
            val restZaaktypeConfiguration = createRestZaaktypeConfiguration(
                restZaaktypeOverzicht = createRestZaaktypeOverzicht(uuid = storedConfiguration.zaaktypeUuid),
                restProcessDefinition = createRestProcessDefinition(key = "fakeCaseDefinitionB")
            ).apply {
                humanTaskParameters = listOf(sharedTaskParameters)
            }

            val zaaktypeConfiguration = restZaaktypeConfigurationConverter.toZaaktypeConfiguration(
                restZaaktypeConfiguration
            )

            then("the row of the shared human task keeps its id with the new settings, and the other row is removed") {
                zaaktypeConfiguration.cmmnExtension?.getHumanTaskParametersCollection()?.single()?.let {
                    it.planItemDefinitionID shouldBe "fakeSharedTask"
                    it.id shouldBe 42L
                    it.doorlooptijd shouldBe 9
                }
                zaaktypeConfiguration.cmmnExtension?.getHumanTaskParametersCollection()?.size shouldBe 1
            }
        }
    }
})
