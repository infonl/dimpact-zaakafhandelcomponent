/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import jakarta.json.bind.JsonbBuilder
import nl.info.zac.admin.ZaaktypeConfigurationBeheerService
import nl.info.zac.admin.model.ZaaktypeCmmnUsereventlistenerParameters
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.admin.model.createZaaktypeBpmnConfiguration
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.app.admin.model.RestHumanTaskParameters
import nl.info.zac.app.admin.model.RestPlanItemDefinition
import nl.info.zac.app.admin.model.RestZaaktypeConfiguration
import nl.info.zac.app.admin.model.createRestProcessDefinition
import nl.info.zac.app.planitems.model.PlanItemType
import nl.info.zac.app.planitems.model.UserEventListenerActie.INTAKE_AFRONDEN
import nl.info.zac.app.planitems.model.UserEventListenerActie.ZAAK_AFHANDELEN
import java.util.UUID

class RestZaaktypeConfigurationConverterUserEventListenerTest : BehaviorSpec({
    val processDefinitionConverter = mockk<RestProcessDefinitionConverter>()
    val restHumanTaskParametersConverter = mockk<RestHumanTaskParametersConverter>()
    val zaaktypeConfigurationBeheerService = mockk<ZaaktypeConfigurationBeheerService>()
    val restZaaktypeConfigurationConverter = RestZaaktypeConfigurationConverter(
        processDefinitionConverter = processDefinitionConverter,
        zaakbeeindigParameterConverter = mockk(),
        humanTaskParametersConverter = restHumanTaskParametersConverter,
        ztcClientService = mockk(),
        resultaattypeReferenceService = mockk(),
        zaaktypeConfigurationBeheerService = zaaktypeConfigurationBeheerService,
        smartDocumentsService = mockk(),
        zaakspecifiekeAutorisatieService = mockk()
    )
    val caseDefinition = createRestProcessDefinition(key = "fakeCaseDefinition").apply {
        userEventListenerDefinitions = listOf(INTAKE_AFRONDEN.name, ZAAK_AFHANDELEN.name).map {
            RestPlanItemDefinition(it, null, PlanItemType.USER_EVENT_LISTENER)
        }
    }

    fun createRestZaaktypeConfigurationWithoutPlanItemSettings(zaaktypeUuid: UUID) =
        JsonbBuilder.create().fromJson(
            """
            {
              "zaaktype": { "uuid": "$zaaktypeUuid", "omschrijving": "fakeOmschrijving" },
              "processDefinition": { "processEngine": "CMMN", "key": "fakeCaseDefinition" },
              "defaultGroepId": "fakeGroupId",
              "smartDocuments": { "isEnabledGlobally": false, "isEnabledForZaaktype": false }
            }
            """.trimIndent(),
            RestZaaktypeConfiguration::class.java
        )

    fun ZaaktypeConfiguration.readUserEventListenerToelichtingen() =
        cmmnExtension?.getUserEventListenerParametersCollection().orEmpty()
            .map { it.planItemDefinitionID to it.toelichting }

    afterEach {
        checkUnnecessaryStub()
    }

    given("a BPMN configuration, and a case definition with user event listeners") {
        val storedConfiguration = createZaaktypeBpmnConfiguration()
        every {
            zaaktypeConfigurationBeheerService.findConfiguration(storedConfiguration.zaaktypeUuid)
        } returns storedConfiguration
        every { processDefinitionConverter.toRestProcessDefinition(any(), true) } returns caseDefinition
        every {
            with(restHumanTaskParametersConverter) { emptyList<RestHumanTaskParameters>().toZaaktypeCmmnHumantaskParameters() }
        } returns emptyList()

        `when`("it is bound to that case definition by a save without plan item settings") {
            val zaaktypeConfiguration = restZaaktypeConfigurationConverter.toZaaktypeConfiguration(
                createRestZaaktypeConfigurationWithoutPlanItemSettings(storedConfiguration.zaaktypeUuid)
            )

            then("it holds a setting without toelichting for each user event listener of the case definition") {
                zaaktypeConfiguration.readUserEventListenerToelichtingen() shouldContainExactlyInAnyOrder listOf(
                    INTAKE_AFRONDEN.name to null,
                    ZAAK_AFHANDELEN.name to null
                )
            }

            then("the user event listener settings that listing the actions of a zaak reads are found") {
                listOf(INTAKE_AFRONDEN, ZAAK_AFHANDELEN).forEach {
                    zaaktypeConfiguration.cmmnExtension?.readUserEventListenerParameters(it.name)?.toelichting shouldBe null
                }
            }
        }
    }

    given("a CMMN configuration with a toelichting for one of the user event listeners of its case definition") {
        val storedConfiguration = createZaaktypeCmmnConfiguration(caseDefinitionId = "fakeCaseDefinition").apply {
            getOrCreateCmmnExtension().setUserEventListenerParametersCollection(
                listOf(
                    ZaaktypeCmmnUsereventlistenerParameters().apply {
                        planItemDefinitionID = INTAKE_AFRONDEN.name
                        toelichting = "fakeToelichting"
                    }
                )
            )
        }
        every {
            zaaktypeConfigurationBeheerService.findConfiguration(storedConfiguration.zaaktypeUuid)
        } returns storedConfiguration
        every { processDefinitionConverter.toRestProcessDefinition(any(), true) } returns caseDefinition

        `when`("it is saved with the same case definition and without plan item settings") {
            val zaaktypeConfiguration = restZaaktypeConfigurationConverter.toZaaktypeConfiguration(
                createRestZaaktypeConfigurationWithoutPlanItemSettings(storedConfiguration.zaaktypeUuid)
            )

            then("the stored toelichting stays, and the other user event listener gets a setting without one") {
                zaaktypeConfiguration.readUserEventListenerToelichtingen() shouldContainExactlyInAnyOrder listOf(
                    INTAKE_AFRONDEN.name to "fakeToelichting",
                    ZAAK_AFHANDELEN.name to null
                )
            }
        }
    }
})
