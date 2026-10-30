/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.admin.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.json.bind.JsonbBuilder
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.shared.ZgwApiService.Companion.ROLTYPE_OMSCHRIJVING_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.util.ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createEigenschap
import nl.info.client.zgw.ztc.model.createResultaatType
import nl.info.client.zgw.ztc.model.createRolType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.client.zgw.ztc.model.generated.OmschrijvingGeneriekEnum
import nl.info.zac.admin.ResultaattypeReferenceService
import nl.info.zac.admin.ZaaktypeConfigurationBeheerService
import nl.info.zac.admin.model.ProcessEngine
import nl.info.zac.admin.model.ZaakafhandelparametersStatusMailOption
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.admin.model.createAutomaticEmailConfirmation
import nl.info.zac.admin.model.createZaaktypeBpmnConfiguration
import nl.info.zac.admin.model.createZaaktypeBrpParameters
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.admin.model.createZaaktypeCompletionParameters
import nl.info.zac.app.admin.model.RestAutomaticEmailConfirmation
import nl.info.zac.app.admin.model.RestHumanTaskParameters
import nl.info.zac.app.admin.model.RestSmartDocuments
import nl.info.zac.app.admin.model.RestZaakAfzender
import nl.info.zac.app.admin.model.RestZaaktypeConfiguration
import nl.info.zac.app.admin.model.createRestProcessDefinition
import nl.info.zac.app.admin.model.createRestResultaattype
import nl.info.zac.app.admin.model.createRestZaakbeeindigParameter
import nl.info.zac.app.admin.model.createRestZaaktypeConfiguration
import nl.info.zac.app.admin.model.createRestZaaktypeOverzicht
import nl.info.zac.app.zaak.model.toRestResultaatType
import nl.info.zac.smartdocuments.SmartDocumentsService
import nl.info.zac.zaak.ZaakspecifiekeAutorisatieService
import java.time.LocalDate
import java.util.UUID

class RestZaakafhandelParametersConverterTest : BehaviorSpec({
    val processDefinitionConverter = mockk<RestProcessDefinitionConverter>()
    val zaakbeeindigParameterConverter = mockk<RestZaakbeeindigParameterConverter>()
    val restHumanTaskParametersConverter = mockk<RestHumanTaskParametersConverter>()
    val ztcClientService = mockk<ZtcClientService>()
    val zaaktypeConfigurationBeheerService = mockk<ZaaktypeConfigurationBeheerService>()
    val smartDocumentsService = mockk<SmartDocumentsService>()
    val zgwApiService = mockk<ZgwApiService>()
    val zaakspecifiekeAutorisatieService = ZaakspecifiekeAutorisatieService(
        zrcClientService = mockk(),
        ztcClientService = ztcClientService,
        zgwApiService = zgwApiService,
        indexingService = mockk(),
        identityService = mockk(),
        flowableTaskService = mockk(),
        taskHistoryService = mockk()
    )

    val resultaattypeReferenceService = mockk<ResultaattypeReferenceService>()
    val restZaaktypeConfigurationConverter = RestZaaktypeConfigurationConverter(
        processDefinitionConverter = processDefinitionConverter,
        zaakbeeindigParameterConverter = zaakbeeindigParameterConverter,
        humanTaskParametersConverter = restHumanTaskParametersConverter,
        ztcClientService = ztcClientService,
        resultaattypeReferenceService = resultaattypeReferenceService,
        zaaktypeConfigurationBeheerService = zaaktypeConfigurationBeheerService,
        smartDocumentsService = smartDocumentsService,
        zaakspecifiekeAutorisatieService = zaakspecifiekeAutorisatieService
    )

    val restCaseDefinition = createRestProcessDefinition(key = "fakeCaseKey", naam = "fakeCaseName")

    afterEach {
        checkUnnecessaryStub()
    }

    given("ZaakafhandelParameters CMMN with minimal content") {
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()
        val zaakType = createZaakType().apply {
            beginGeldigheid = LocalDate.now().minusDays(1)
        }
        val resultaatType = createResultaatType()
        val restResultType = resultaatType.toRestResultaatType()
        val restZaakbeeindigParameter = createRestZaakbeeindigParameter(resultaattype = restResultType)

        every { ztcClientService.readZaaktype(zaaktypeCmmnConfiguration.zaaktypeUuid) } returns zaakType
        every {
            resultaattypeReferenceService.findNietOntvankelijkResultaattype(zaaktypeCmmnConfiguration)
        } returns resultaatType
        every {
            zaakbeeindigParameterConverter.convertZaakbeeindigParameters(zaaktypeCmmnConfiguration.getZaakbeeindigParameters())
        } returns listOf(restZaakbeeindigParameter)
        every { smartDocumentsService.isEnabled() } returns true
        every {
            processDefinitionConverter.toRestProcessDefinition(zaaktypeCmmnConfiguration.processBinding!!, true)
        } returns restCaseDefinition
        every {
            restHumanTaskParametersConverter.convertHumanTaskParametersCollection(any(), emptyList())
        } returns emptyList()
        every { ztcClientService.findEigenschap(zaakType.url, ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD) } returns null

        `when`("converted to REST representation") {
            val restZaakafhandelParameters = restZaaktypeConfigurationConverter.toRestZaaktypeConfiguration(
                zaaktypeCmmnConfiguration,
                true
            )

            then("the created object is correct") {
                with(restZaakafhandelParameters) {
                    id shouldBe zaaktypeCmmnConfiguration.id
                    with(zaaktype) {
                        uuid shouldBe zaakType.url.extractUuid()
                        identificatie shouldBe zaakType.identificatie
                        doel shouldBe zaakType.doel
                        omschrijving shouldBe zaakType.omschrijving
                        hasServicenorm shouldBe false
                        versiedatum shouldBe zaakType.versiedatum
                        beginGeldigheid shouldBe zaakType.beginGeldigheid
                        eindeGeldigheid shouldBe zaakType.eindeGeldigheid
                        vertrouwelijkheidaanduiding?.name shouldBe zaakType.vertrouwelijkheidaanduiding?.name
                        isNuGeldig shouldBe true
                    }
                    processDefinition shouldBe restCaseDefinition
                    defaultBehandelaarId shouldBe null
                    defaultGroepId shouldBe null
                    einddatumGeplandWaarschuwing shouldBe null
                    uiterlijkeEinddatumAfdoeningWaarschuwing shouldBe null
                    creatiedatum shouldNotBe null
                    zaakNietOntvankelijkResultaattype shouldBe restResultType
                    productaanvraagtype shouldBe null
                    isValide shouldBe false
                    isZaakspecifiekAutoriseerbaar shouldBe false
                    mailtemplateKoppelingen shouldHaveSize 1
                    zaakbeeindigParameters shouldBe listOf(restZaakbeeindigParameter)
                    zaakAfzenders shouldBe listOf(
                        RestZaakAfzender(id = null, mail = "mail@example.com", replyTo = "replyTo@example.com"),
                        RestZaakAfzender(mail = "GEMEENTE", isSpeciaal = true),
                        RestZaakAfzender(mail = "MEDEWERKER", isSpeciaal = true)
                    )
                    smartDocuments shouldBe RestSmartDocuments(
                        isEnabledGlobally = true,
                        isEnabledForZaaktype = false
                    )
                }
            }

            and("it holds the settings of the plan items of the case definition, with the default status mails") {
                with(restZaakafhandelParameters) {
                    intakeMail shouldBe ZaakafhandelparametersStatusMailOption.BESCHIKBAAR_UIT
                    afrondenMail shouldBe ZaakafhandelparametersStatusMailOption.BESCHIKBAAR_UIT
                    humanTaskParameters shouldBe emptyList()
                    userEventListenerParameters shouldBe emptyList()
                }
            }
        }
    }

    given("RestZaakafhandelParameters CMMN with minimal content") {
        val restResultType = createResultaatType().toRestResultaatType()
        val restZaakafhandelParameters = createRestZaaktypeConfiguration().apply {
            processDefinition = createRestProcessDefinition(key = "fakeCaseDefinitionKey")
            zaakNietOntvankelijkResultaattype = restResultType
        }
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()
        every {
            zaaktypeConfigurationBeheerService.findConfiguration(restZaakafhandelParameters.zaaktype.uuid)
        } returns zaaktypeCmmnConfiguration
        every {
            with(restHumanTaskParametersConverter) { any<List<RestHumanTaskParameters>>().toZaaktypeCmmnHumantaskParameters() }
        } returns emptyList()
        every { resultaattypeReferenceService.readOmschrijving(restResultType.id) } returns "fakeNietOntvankelijk"
        every { zaakbeeindigParameterConverter.toZaaktypeCompletionParameters(any()) } returns emptyList()

        `when`("converted to DB model representation") {
            val zaaktypeCmmnConfiguration = restZaaktypeConfigurationConverter.toZaaktypeConfiguration(
                restZaakafhandelParameters
            )

            then("the created object is correct") {
                with(zaaktypeCmmnConfiguration) {
                    id shouldBe restZaakafhandelParameters.id
                    zaaktypeUuid shouldBe restZaakafhandelParameters.zaaktype.uuid
                    zaaktypeOmschrijving shouldBe "fakeOmschrijving"
                    processBinding?.processEngine shouldBe ProcessEngine.CMMN
                    processBinding?.definitionKey shouldBe "fakeCaseDefinitionKey"
                    groepID shouldBe "fakeGroupId"
                    defaultBehandelaarId shouldBe null
                    einddatumGeplandWaarschuwing shouldBe null
                    uiterlijkeEinddatumAfdoeningWaarschuwing shouldBe null
                    nietOntvankelijkResultaattypeOmschrijving shouldBe "fakeNietOntvankelijk"
                    creatiedatum shouldNotBe null
                    cmmnExtension?.intakeMail shouldBe ZaakafhandelparametersStatusMailOption.BESCHIKBAAR_UIT.name
                    cmmnExtension?.afrondenMail shouldBe ZaakafhandelparametersStatusMailOption.BESCHIKBAAR_UIT.name
                    productaanvraagtype shouldBe null
                    isSmartDocumentsEnabled shouldBe false
                }
            }
        }
    }

    given(
        """
        a zaaktype without a stored configuration,
        and a request that binds it to a CMMN case definition and leaves out the intake and afronden mail
        """
    ) {
        val restZaaktypeConfiguration = createRestZaaktypeConfiguration(
            id = null,
            restProcessDefinition = createRestProcessDefinition(key = "fakeCaseDefinitionKey")
        )
        every {
            zaaktypeConfigurationBeheerService.findConfiguration(restZaaktypeConfiguration.zaaktype.uuid)
        } returns null
        every {
            with(restHumanTaskParametersConverter) { any<List<RestHumanTaskParameters>>().toZaaktypeCmmnHumantaskParameters() }
        } returns emptyList()
        every { zaakbeeindigParameterConverter.toZaaktypeCompletionParameters(emptyList()) } returns emptyList()

        `when`("converted to DB model representation") {
            val zaaktypeConfiguration = restZaaktypeConfigurationConverter.toZaaktypeConfiguration(
                restZaaktypeConfiguration
            )

            then(
                """
                the new CMMN extension has the intake and afronden mail 'beschikbaar uit',
                so that the configuration satisfies the non-null database columns of these settings
                """
            ) {
                with(zaaktypeConfiguration.cmmnExtension!!) {
                    intakeMail shouldBe ZaakafhandelparametersStatusMailOption.BESCHIKBAAR_UIT.name
                    afrondenMail shouldBe ZaakafhandelparametersStatusMailOption.BESCHIKBAAR_UIT.name
                }
            }
        }
    }

    given(
        """
        a stored CMMN configuration with the intake and afronden mail 'beschikbaar aan',
        and a request that leaves out the intake and afronden mail
        """
    ) {
        val storedConfiguration = createZaaktypeCmmnConfiguration().apply {
            cmmnExtension!!.intakeMail = ZaakafhandelparametersStatusMailOption.BESCHIKBAAR_AAN.name
            cmmnExtension!!.afrondenMail = ZaakafhandelparametersStatusMailOption.BESCHIKBAAR_AAN.name
        }
        val restZaaktypeConfiguration = createRestZaaktypeConfiguration(
            restZaaktypeOverzicht = createRestZaaktypeOverzicht(uuid = storedConfiguration.zaaktypeUuid),
            restProcessDefinition = createRestProcessDefinition(key = "fakeCaseDefinitionKey")
        )
        every { zaaktypeConfigurationBeheerService.findConfiguration(storedConfiguration.zaaktypeUuid) } returns
            storedConfiguration
        every {
            with(restHumanTaskParametersConverter) { any<List<RestHumanTaskParameters>>().toZaaktypeCmmnHumantaskParameters() }
        } returns emptyList()
        every { zaakbeeindigParameterConverter.toZaaktypeCompletionParameters(emptyList()) } returns emptyList()

        `when`("converted to DB model representation") {
            val zaaktypeConfiguration = restZaaktypeConfigurationConverter.toZaaktypeConfiguration(
                restZaaktypeConfiguration
            )

            then("the intake and afronden mail keep their stored values") {
                with(zaaktypeConfiguration.cmmnExtension!!) {
                    intakeMail shouldBe ZaakafhandelparametersStatusMailOption.BESCHIKBAAR_AAN.name
                    afrondenMail shouldBe ZaakafhandelparametersStatusMailOption.BESCHIKBAAR_AAN.name
                }
            }
        }
    }

    given(
        """
        a stored BPMN configuration with confirmation email parameters and BRP doelbindingen,
        and a request that leaves out every setting that the OpenAPI spec marks optional
        """
    ) {
        val zaaktypeUuid = UUID.randomUUID()
        val storedBrpParameters = createZaaktypeBrpParameters()
        val storedConfiguration = createZaaktypeBpmnConfiguration(
            zaaktypeUUID = zaaktypeUuid,
            zaaktypeBrpParameters = storedBrpParameters
        ).apply {
            zaaktypeEmailParameters = createAutomaticEmailConfirmation(zaaktypeConfiguration = this)
        }
        val storedEmailParameters = storedConfiguration.zaaktypeEmailParameters
        val restZaaktypeConfiguration = JsonbBuilder.create().fromJson(
            """
            {
              "zaaktype": { "uuid": "$zaaktypeUuid", "omschrijving": "fakeOmschrijving" },
              "processDefinition": { "processEngine": "BPMN", "key": "fakeBpmnProcessDefinitionKey" },
              "defaultGroepId": "fakeGroupId",
              "smartDocuments": { "isEnabledGlobally": false, "isEnabledForZaaktype": false }
            }
            """.trimIndent(),
            RestZaaktypeConfiguration::class.java
        )
        every { zaaktypeConfigurationBeheerService.findConfiguration(zaaktypeUuid) } returns storedConfiguration

        `when`("converted to DB model representation") {
            val zaaktypeConfiguration = restZaaktypeConfigurationConverter.toZaaktypeConfiguration(
                restZaaktypeConfiguration
            )

            then("the left-out settings keep their stored values") {
                with(zaaktypeConfiguration) {
                    groepID shouldBe "fakeGroupId"
                    getAutomaticEmailConfirmation() shouldBe storedEmailParameters
                    zaaktypeBrpParameters shouldBe storedBrpParameters
                }
            }
        }
    }

    given("ZaakafhandelParameters BPMN with minimal content") {
        val zaaktypeBpmnConfiguration = createZaaktypeBpmnConfiguration()
        val restBpmnProcessDefinition = createRestProcessDefinition(processEngine = ProcessEngine.BPMN)
        val zaakType = createZaakType().apply {
            beginGeldigheid = LocalDate.now().minusDays(1)
        }
        val resultaatType = createResultaatType()
        val restResultType = resultaatType.toRestResultaatType()
        val restZaakbeeindigParameter = createRestZaakbeeindigParameter(resultaattype = restResultType)

        every { ztcClientService.readZaaktype(zaaktypeBpmnConfiguration.zaaktypeUuid) } returns zaakType
        every {
            resultaattypeReferenceService.findNietOntvankelijkResultaattype(zaaktypeBpmnConfiguration)
        } returns resultaatType
        every {
            zaakbeeindigParameterConverter.convertZaakbeeindigParameters(zaaktypeBpmnConfiguration.getZaakbeeindigParameters())
        } returns listOf(restZaakbeeindigParameter)
        every { smartDocumentsService.isEnabled() } returns true
        every {
            processDefinitionConverter.toRestProcessDefinition(zaaktypeBpmnConfiguration.processBinding!!, true)
        } returns restBpmnProcessDefinition
        every {
            restHumanTaskParametersConverter.convertHumanTaskParametersCollection(emptySet(), emptyList())
        } returns emptyList()
        every { ztcClientService.findEigenschap(zaakType.url, ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD) } returns null

        `when`("converted to REST representation") {
            val restZaakafhandelParameters = restZaaktypeConfigurationConverter.toRestZaaktypeConfiguration(
                zaaktypeBpmnConfiguration,
                true
            )

            then("the created object is correct") {
                with(restZaakafhandelParameters) {
                    id shouldBe zaaktypeBpmnConfiguration.id
                    with(zaaktype) {
                        uuid shouldBe zaakType.url.extractUuid()
                        identificatie shouldBe zaakType.identificatie
                        doel shouldBe zaakType.doel
                        omschrijving shouldBe zaakType.omschrijving
                        hasServicenorm shouldBe false
                        versiedatum shouldBe zaakType.versiedatum
                        beginGeldigheid shouldBe zaakType.beginGeldigheid
                        eindeGeldigheid shouldBe zaakType.eindeGeldigheid
                        vertrouwelijkheidaanduiding?.name shouldBe zaakType.vertrouwelijkheidaanduiding?.name
                        isNuGeldig shouldBe true
                    }
                    defaultGroepId shouldBe null
                    creatiedatum shouldNotBe null
                    zaakNietOntvankelijkResultaattype shouldBe restResultType
                    productaanvraagtype shouldBe null
                    zaakbeeindigParameters shouldBe listOf(restZaakbeeindigParameter)
                    isZaakspecifiekAutoriseerbaar shouldBe false
                    smartDocuments shouldBe RestSmartDocuments(
                        isEnabledGlobally = true,
                        isEnabledForZaaktype = false
                    )
                    processDefinition shouldBe restBpmnProcessDefinition
                }
            }

            and("it has no settings of plan items, as its process definition has no plan items") {
                with(restZaakafhandelParameters) {
                    intakeMail shouldBe null
                    afrondenMail shouldBe null
                    humanTaskParameters shouldBe emptyList()
                    userEventListenerParameters shouldBe emptyList()
                }
            }
        }
    }

    given("a zaaktype that has both the 'ZAAK_GEAUTORISEERD' eigenschap and the zaakspecifiek geautoriseerde medewerker roltype") {
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()
        val zaakType = createZaakType()
        val resultaatType = createResultaatType()

        every { ztcClientService.readZaaktype(zaaktypeCmmnConfiguration.zaaktypeUuid) } returns zaakType
        every {
            resultaattypeReferenceService.findNietOntvankelijkResultaattype(zaaktypeCmmnConfiguration)
        } returns resultaatType
        every {
            zaakbeeindigParameterConverter.convertZaakbeeindigParameters(zaaktypeCmmnConfiguration.getZaakbeeindigParameters())
        } returns emptyList()
        every { smartDocumentsService.isEnabled() } returns true
        every {
            processDefinitionConverter.toRestProcessDefinition(zaaktypeCmmnConfiguration.processBinding!!, true)
        } returns restCaseDefinition
        every {
            restHumanTaskParametersConverter.convertHumanTaskParametersCollection(any(), emptyList())
        } returns emptyList()
        every { ztcClientService.findEigenschap(zaakType.url, ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD) } returns
            createEigenschap(naam = ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD)
        every { zgwApiService.findZaakspecifiekGeautoriseerdeMedewerkerRoltype(zaakType.url) } returns createRolType(
            omschrijving = ROLTYPE_OMSCHRIJVING_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER,
            omschrijvingGeneriek = OmschrijvingGeneriekEnum.BEHANDELAAR,
            zaakTypeUri = zaakType.url
        )

        `when`("converted to REST representation") {
            val restZaakafhandelParameters = restZaaktypeConfigurationConverter.toRestZaaktypeConfiguration(
                zaaktypeCmmnConfiguration,
                true
            )

            then("the zaaktype configuration is marked as 'zaakspecifiek autoriseerbaar'") {
                restZaakafhandelParameters.isZaakspecifiekAutoriseerbaar shouldBe true
            }
        }
    }

    given("a zaaktype that has the 'ZAAK_GEAUTORISEERD' eigenschap but no zaakspecifiek geautoriseerde medewerker roltype") {
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()
        val zaakType = createZaakType()
        val resultaatType = createResultaatType()

        every { ztcClientService.readZaaktype(zaaktypeCmmnConfiguration.zaaktypeUuid) } returns zaakType
        every {
            resultaattypeReferenceService.findNietOntvankelijkResultaattype(zaaktypeCmmnConfiguration)
        } returns resultaatType
        every {
            zaakbeeindigParameterConverter.convertZaakbeeindigParameters(zaaktypeCmmnConfiguration.getZaakbeeindigParameters())
        } returns emptyList()
        every { smartDocumentsService.isEnabled() } returns true
        every {
            processDefinitionConverter.toRestProcessDefinition(zaaktypeCmmnConfiguration.processBinding!!, true)
        } returns restCaseDefinition
        every {
            restHumanTaskParametersConverter.convertHumanTaskParametersCollection(any(), emptyList())
        } returns emptyList()
        every { ztcClientService.findEigenschap(zaakType.url, ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD) } returns
            createEigenschap(naam = ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD)
        every { zgwApiService.findZaakspecifiekGeautoriseerdeMedewerkerRoltype(zaakType.url) } returns null

        `when`("converted to REST representation") {
            val restZaakafhandelParameters = restZaaktypeConfigurationConverter.toRestZaaktypeConfiguration(
                zaaktypeCmmnConfiguration,
                true
            )

            then("the zaaktype configuration is not marked as 'zaakspecifiek autoriseerbaar'") {
                restZaakafhandelParameters.isZaakspecifiekAutoriseerbaar shouldBe false
            }
        }
    }

    given("a zaaktype CMMN configuration and the zaaktype it belongs to") {
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()
        val zaakType = createZaakType()
        val resultaatType = createResultaatType()

        every {
            resultaattypeReferenceService.findNietOntvankelijkResultaattype(zaaktypeCmmnConfiguration)
        } returns resultaatType
        every {
            zaakbeeindigParameterConverter.convertZaakbeeindigParameters(zaaktypeCmmnConfiguration.getZaakbeeindigParameters())
        } returns emptyList()
        every { smartDocumentsService.isEnabled() } returns true
        every {
            processDefinitionConverter.toRestProcessDefinition(zaaktypeCmmnConfiguration.processBinding!!, true)
        } returns restCaseDefinition
        every {
            restHumanTaskParametersConverter.convertHumanTaskParametersCollection(any(), emptyList())
        } returns emptyList()
        every { ztcClientService.findEigenschap(zaakType.url, ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD) } returns null

        `when`("converted to REST representation with the zaaktype passed in") {
            val restZaakafhandelParameters = restZaaktypeConfigurationConverter.toRestZaaktypeConfiguration(
                zaaktypeCmmnConfiguration,
                true,
                zaakType
            )

            then("the passed zaaktype is used and the zaaktype is not retrieved from the ZTC") {
                restZaakafhandelParameters.zaaktype.uuid shouldBe zaakType.url.extractUuid()
                verify(exactly = 0) { ztcClientService.readZaaktype(any<UUID>()) }
            }
        }
    }

    given("a zaaktype configuration that was never saved") {
        val zaaktypeConfiguration = ZaaktypeConfiguration().apply { zaaktypeUuid = UUID.randomUUID() }
        val zaakType = createZaakType()
        every { smartDocumentsService.isEnabled() } returns true
        every { ztcClientService.findEigenschap(zaakType.url, ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD) } returns null

        `when`("converted to REST representation") {
            val restZaaktypeConfiguration = restZaaktypeConfigurationConverter.toRestZaaktypeConfiguration(
                zaaktypeConfiguration,
                false,
                zaakType
            )

            then("it names no process definition") {
                restZaaktypeConfiguration.processDefinition shouldBe null
            }
        }
    }

    context("toZaaktypeConfiguration conversion") {
        given("a complete REST zaaktype configuration") {
            val restZaaktypeConfig = RestZaaktypeConfiguration(
                id = 456L,
                zaaktype = createRestZaaktypeOverzicht(),
                processDefinition = createRestProcessDefinition(key = "fakeCaseDefinitionKey"),
                intakeMail = ZaakafhandelparametersStatusMailOption.BESCHIKBAAR_AAN,
                afrondenMail = ZaakafhandelparametersStatusMailOption.BESCHIKBAAR_UIT,
                defaultGroepId = "fakeDefaultGroup",
                defaultBehandelaarId = "fakeBehandelaar",
                productaanvraagtype = "  fakeProductaanvraag  ",
                einddatumGeplandWaarschuwing = 4,
                uiterlijkeEinddatumAfdoeningWaarschuwing = 8,
                zaakNietOntvankelijkResultaattype = createRestResultaattype(),
                zaakbeeindigParameters = listOf(createRestZaakbeeindigParameter()),
                smartDocuments = RestSmartDocuments(isEnabledGlobally = true, isEnabledForZaaktype = true)
            )
            val baseConfig = ZaaktypeConfiguration().apply {
                zaaktypeUuid = restZaaktypeConfig.zaaktype.uuid
            }

            every {
                zaaktypeConfigurationBeheerService.findConfiguration(restZaaktypeConfig.zaaktype.uuid)
            } returns baseConfig
            every {
                with(restHumanTaskParametersConverter) {
                    any<List<RestHumanTaskParameters>>().toZaaktypeCmmnHumantaskParameters()
                }
            } returns emptyList()
            every {
                resultaattypeReferenceService.readOmschrijving(restZaaktypeConfig.zaakNietOntvankelijkResultaattype!!.id)
            } returns "fakeNietOntvankelijk"
            every {
                zaakbeeindigParameterConverter.toZaaktypeCompletionParameters(restZaaktypeConfig.zaakbeeindigParameters)
            } returns listOf(createZaaktypeCompletionParameters(resultaattypeOmschrijving = "fakeToegekend"))

            `when`("converting REST configuration to ZaaktypeConfiguration entity") {
                val entity = restZaaktypeConfigurationConverter.toZaaktypeConfiguration(restZaaktypeConfig)

                then("all properties and CMMN extension parameters are mapped") {
                    entity.id shouldBe 456L
                    entity.zaaktypeUuid shouldBe restZaaktypeConfig.zaaktype.uuid
                    entity.zaaktypeOmschrijving shouldBe restZaaktypeConfig.zaaktype.omschrijving
                    entity.getProcessEngine() shouldBe ProcessEngine.CMMN
                    entity.processBinding?.definitionKey shouldBe "fakeCaseDefinitionKey"
                    entity.groepID shouldBe "fakeDefaultGroup"
                    entity.defaultBehandelaarId shouldBe "fakeBehandelaar"
                    entity.productaanvraagtype shouldBe "fakeProductaanvraag"
                    entity.einddatumGeplandWaarschuwing shouldBe 4
                    entity.uiterlijkeEinddatumAfdoeningWaarschuwing shouldBe 8
                    entity.cmmnExtension?.intakeMail shouldBe "BESCHIKBAAR_AAN"
                    entity.cmmnExtension?.afrondenMail shouldBe "BESCHIKBAAR_UIT"
                }

                and("the resultaattypen are referenced by the omschrijving that ZTC has for their UUID") {
                    entity.nietOntvankelijkResultaattypeOmschrijving shouldBe "fakeNietOntvankelijk"
                    entity.getZaakbeeindigParameters().single().resultaattypeOmschrijving shouldBe "fakeToegekend"
                }
            }
        }

        given("a stored BPMN configuration, and a BPMN REST configuration for it") {
            val storedConfiguration = createZaaktypeBpmnConfiguration()
            val restZaaktypeConfiguration = createRestZaaktypeConfiguration(
                restZaaktypeOverzicht = createRestZaaktypeOverzicht(uuid = storedConfiguration.zaaktypeUuid),
                restProcessDefinition = createRestProcessDefinition(
                    processEngine = ProcessEngine.BPMN,
                    key = "fakeOtherProcessDefinitionKey"
                )
            ).apply {
                einddatumGeplandWaarschuwing = 5
                uiterlijkeEinddatumAfdoeningWaarschuwing = 2
                intakeMail = ZaakafhandelparametersStatusMailOption.BESCHIKBAAR_AAN
                automaticEmailConfirmation = RestAutomaticEmailConfirmation(templateName = "fakeTemplateName")
            }
            every {
                zaaktypeConfigurationBeheerService.findConfiguration(storedConfiguration.zaaktypeUuid)
            } returns storedConfiguration
            every { zaakbeeindigParameterConverter.toZaaktypeCompletionParameters(emptyList()) } returns emptyList()

            `when`("converting REST configuration to ZaaktypeConfiguration entity") {
                val zaaktypeConfiguration = restZaaktypeConfigurationConverter.toZaaktypeConfiguration(
                    restZaaktypeConfiguration
                )

                then("it is bound to the BPMN process definition, without settings of plan items") {
                    zaaktypeConfiguration.getProcessEngine() shouldBe ProcessEngine.BPMN
                    zaaktypeConfiguration.processBinding?.definitionKey shouldBe "fakeOtherProcessDefinitionKey"
                    zaaktypeConfiguration.cmmnExtension shouldBe null
                }

                and("the deadline warning windows are stored as the payload sets them") {
                    zaaktypeConfiguration.einddatumGeplandWaarschuwing shouldBe 5
                    zaaktypeConfiguration.uiterlijkeEinddatumAfdoeningWaarschuwing shouldBe 2
                }

                and("the confirmation email parameters are stored as the payload sets them") {
                    zaaktypeConfiguration.zaaktypeEmailParameters?.templateName shouldBe "fakeTemplateName"
                }
            }
        }
    }
})
