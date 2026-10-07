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
import nl.info.zac.app.admin.converter.RestCaseDefinitionConverter
import net.atos.zac.app.admin.converter.RESTHumanTaskParametersConverter
import net.atos.zac.app.admin.model.RESTCaseDefinition
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
import nl.info.zac.admin.ZaaktypeConfigurationBeheerService
import nl.info.zac.admin.model.ZaakafhandelparametersStatusMailOption
import nl.info.zac.admin.model.ProcessEngine
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.admin.model.createZaaktypeBpmnConfiguration
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.app.admin.model.RestSmartDocuments
import nl.info.zac.app.admin.model.RestZaakAfzender
import nl.info.zac.app.admin.model.RestZaaktypeConfiguration
import nl.info.zac.app.admin.model.createRestResultaattype
import nl.info.zac.app.admin.model.createRestZaaktypeConfiguration
import nl.info.zac.app.admin.model.createRestZaaktypeOverzicht
import nl.info.zac.app.admin.model.createRestZaakbeeindigParameter
import nl.info.zac.app.zaak.model.toRestResultaatType
import nl.info.zac.smartdocuments.SmartDocumentsService
import nl.info.zac.zaak.ZaakspecifiekeAutorisatieService
import java.time.LocalDate

class RestZaakafhandelParametersConverterTest : BehaviorSpec({
    val caseDefinitionConverter = mockk<RestCaseDefinitionConverter>()
    val zaakbeeindigParameterConverter = mockk<RestZaakbeeindigParameterConverter>()
    val restHumanTaskParametersConverter = mockk<RESTHumanTaskParametersConverter>()
    val ztcClientService = mockk<ZtcClientService>()
    val zaaktypeConfigurationBeheerService = mockk<ZaaktypeConfigurationBeheerService>()
    val smartDocumentsService = mockk<SmartDocumentsService>()
    val zgwApiService = mockk<ZgwApiService>()
    val zaakspecifiekeAutorisatieService = ZaakspecifiekeAutorisatieService(
        zrcClientService = mockk(),
        ztcClientService = ztcClientService,
        zgwApiService = zgwApiService,
        indexingService = mockk()
    )

    val restZaaktypeConfigurationConverter = RestZaaktypeConfigurationConverter(
        caseDefinitionConverter = caseDefinitionConverter,
        zaakbeeindigParameterConverter = zaakbeeindigParameterConverter,
        humanTaskParametersConverter = restHumanTaskParametersConverter,
        ztcClientService = ztcClientService,
        zaaktypeConfigurationBeheerService = zaaktypeConfigurationBeheerService,
        smartDocumentsService = smartDocumentsService,
        zaakspecifiekeAutorisatieService = zaakspecifiekeAutorisatieService
    )

    val restCaseDefinition = RESTCaseDefinition("fakeCaseName", "fakeCaseKey").apply {
        humanTaskDefinitions = emptyList()
        userEventListenerDefinitions = emptyList()
    }

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
            ztcClientService.readResultaattype(zaaktypeCmmnConfiguration.nietOntvankelijkResultaattype!!)
        } returns resultaatType
        every {
            zaakbeeindigParameterConverter.convertZaakbeeindigParameters(zaaktypeCmmnConfiguration.getZaakbeeindigParameters())
        } returns listOf(restZaakbeeindigParameter)
        every { smartDocumentsService.isEnabled() } returns true
        every {
            caseDefinitionConverter.convertToRestCaseDefinition(
                "fakeCaseDefinitionId",
                true
            )
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
                    caseDefinition shouldBe restCaseDefinition
                    defaultBehandelaarId shouldBe null
                    defaultGroepId shouldBe null
                    einddatumGeplandWaarschuwing shouldBe null
                    uiterlijkeEinddatumAfdoeningWaarschuwing shouldBe null
                    creatiedatum shouldNotBe null
                    zaakNietOntvankelijkResultaattype shouldBe restResultType
                    // default value should be set
                    intakeMail shouldBe ZaakafhandelparametersStatusMailOption.BESCHIKBAAR_UIT
                    // default value should be set
                    afrondenMail shouldBe ZaakafhandelparametersStatusMailOption.BESCHIKBAAR_UIT
                    productaanvraagtype shouldBe null
                    isValide shouldBe false
                    isZaakspecifiekAutoriseerbaar shouldBe false
                    humanTaskParameters shouldBe emptyList()
                    userEventListenerParameters shouldBe emptyList()
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
        }
    }

    given("RestZaakafhandelParameters CMMN with minimal content") {
        val restResultType = createResultaatType().toRestResultaatType()
        val restZaakafhandelParameters = createRestZaaktypeConfiguration().apply {
            caseDefinition = RESTCaseDefinition().apply { key = "fakeCaseDefinitionKey" }
            zaakNietOntvankelijkResultaattype = restResultType
        }
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration()
        every {
            zaaktypeConfigurationBeheerService.findConfiguration(restZaakafhandelParameters.zaaktype.uuid)
        } returns zaaktypeCmmnConfiguration
        every { restHumanTaskParametersConverter.convertRESTHumanTaskParameters(any()) } returns emptyList()

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
                    nietOntvankelijkResultaattype shouldBe restResultType.id
                    creatiedatum shouldNotBe null
                    cmmnExtension?.intakeMail shouldBe null
                    cmmnExtension?.afrondenMail shouldBe null
                    productaanvraagtype shouldBe null
                    isSmartDocumentsEnabled shouldBe false
                }
            }
        }
    }

    given("ZaakafhandelParameters BPMN with minimal content") {
        val zaaktypeBpmnConfiguration = createZaaktypeBpmnConfiguration()
        val zaakType = createZaakType().apply {
            beginGeldigheid = LocalDate.now().minusDays(1)
        }
        val resultaatType = createResultaatType()
        val restResultType = resultaatType.toRestResultaatType()
        val restZaakbeeindigParameter = createRestZaakbeeindigParameter(resultaattype = restResultType)

        every { ztcClientService.readZaaktype(zaaktypeBpmnConfiguration.zaaktypeUuid) } returns zaakType
        every {
            ztcClientService.readResultaattype(zaaktypeBpmnConfiguration.nietOntvankelijkResultaattype!!)
        } returns resultaatType
        every {
            zaakbeeindigParameterConverter.convertZaakbeeindigParameters(zaaktypeBpmnConfiguration.getZaakbeeindigParameters())
        } returns listOf(restZaakbeeindigParameter)
        every { smartDocumentsService.isEnabled() } returns true
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
            ztcClientService.readResultaattype(zaaktypeCmmnConfiguration.nietOntvankelijkResultaattype!!)
        } returns resultaatType
        every {
            zaakbeeindigParameterConverter.convertZaakbeeindigParameters(zaaktypeCmmnConfiguration.getZaakbeeindigParameters())
        } returns emptyList()
        every { smartDocumentsService.isEnabled() } returns true
        every {
            caseDefinitionConverter.convertToRestCaseDefinition("fakeCaseDefinitionId", true)
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
            ztcClientService.readResultaattype(zaaktypeCmmnConfiguration.nietOntvankelijkResultaattype!!)
        } returns resultaatType
        every {
            zaakbeeindigParameterConverter.convertZaakbeeindigParameters(zaaktypeCmmnConfiguration.getZaakbeeindigParameters())
        } returns emptyList()
        every { smartDocumentsService.isEnabled() } returns true
        every {
            caseDefinitionConverter.convertToRestCaseDefinition("fakeCaseDefinitionId", true)
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

    context("toZaaktypeConfiguration conversion") {
        given("a complete REST zaaktype configuration") {
            val restZaaktypeConfig = RestZaaktypeConfiguration(
                id = 456L,
                zaaktype = createRestZaaktypeOverzicht(),
                caseDefinition = RESTCaseDefinition().apply { key = "fakeCaseDefinitionKey" },
                defaultGroepId = "fakeDefaultGroup",
                defaultBehandelaarId = "fakeBehandelaar",
                productaanvraagtype = "  fakeProductaanvraag  ",
                einddatumGeplandWaarschuwing = 4,
                uiterlijkeEinddatumAfdoeningWaarschuwing = 8,
                zaakNietOntvankelijkResultaattype = createRestResultaattype(),
                intakeMail = ZaakafhandelparametersStatusMailOption.BESCHIKBAAR_AAN,
                afrondenMail = ZaakafhandelparametersStatusMailOption.BESCHIKBAAR_UIT,
                smartDocuments = RestSmartDocuments(isEnabledGlobally = true, isEnabledForZaaktype = true)
            )
            val baseConfig = ZaaktypeConfiguration().apply {
                zaaktypeUuid = restZaaktypeConfig.zaaktype.uuid
            }

            every {
                zaaktypeConfigurationBeheerService.findConfiguration(restZaaktypeConfig.zaaktype.uuid)
            } returns baseConfig
            every {
                restHumanTaskParametersConverter.convertRESTHumanTaskParameters(any())
            } returns emptyList()

            `when`("converting REST configuration to ZaaktypeConfiguration entity") {
                val entity = restZaaktypeConfigurationConverter.toZaaktypeConfiguration(restZaaktypeConfig)

                then("all properties and CMMN extension parameters are mapped") {
                    entity.id shouldBe 456L
                    entity.zaaktypeUuid shouldBe restZaaktypeConfig.zaaktype.uuid
                    entity.zaaktypeOmschrijving shouldBe restZaaktypeConfig.zaaktype.omschrijving
                    entity.getProcessEngine() shouldBe ProcessEngine.CMMN
                    entity.processBinding?.definitionKey shouldBe restZaaktypeConfig.caseDefinition?.key
                    entity.groepID shouldBe "fakeDefaultGroup"
                    entity.defaultBehandelaarId shouldBe "fakeBehandelaar"
                    entity.productaanvraagtype shouldBe "fakeProductaanvraag"
                    entity.einddatumGeplandWaarschuwing shouldBe 4
                    entity.uiterlijkeEinddatumAfdoeningWaarschuwing shouldBe 8
                    entity.cmmnExtension?.intakeMail shouldBe "BESCHIKBAAR_AAN"
                    entity.cmmnExtension?.afrondenMail shouldBe "BESCHIKBAAR_UIT"
                }
            }
        }
    }
})
