/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.admin

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createResultaatType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.admin.ReferenceTableService
import nl.info.zac.admin.ZaaktypeConfigurationBeheerService
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.admin.model.ProcessEngine
import nl.info.zac.admin.model.ReferenceTable.SystemReferenceTable.AFZENDER
import nl.info.zac.admin.model.ZaakbeeindigReden
import nl.info.zac.admin.model.ZaaktypeConfiguration
import nl.info.zac.admin.model.createReferenceTable
import nl.info.zac.admin.model.createReferenceTableValue
import nl.info.zac.admin.model.createZaaktypeBpmnConfiguration
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.admin.model.createZaaktypeConfigurationsUnderTest
import nl.info.zac.app.admin.converter.RestProcessDefinitionConverter
import nl.info.zac.app.admin.converter.RestZaaktypeConfigurationConverter
import nl.info.zac.app.admin.model.createRestProcessDefinition
import nl.info.zac.app.admin.model.createRestZaaktypeConfiguration
import nl.info.zac.app.admin.model.createRestZaaktypeOverzicht
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.exception.ErrorCode.ERROR_CODE_PRODUCTAANVRAAGTYPE_ALREADY_IN_USE
import nl.info.zac.exception.ErrorCode.ERROR_CODE_USER_NOT_IN_GROUP
import nl.info.zac.exception.ErrorCode.ERROR_CODE_VALIDATION_GENERIC
import nl.info.zac.exception.InputValidationFailedException
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.exception.UserNotInGroupException
import nl.info.zac.policy.PolicyService
import nl.info.zac.smartdocuments.SmartDocumentsTemplatesService
import nl.info.zac.smartdocuments.exception.SmartDocumentsConfigurationException
import java.net.URI
import java.util.UUID

class ZaaktypeConfigurationRestServiceTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val configurationService = mockk<ConfigurationService>()
    val zaaktypeConfigurationBeheerService = mockk<ZaaktypeConfigurationBeheerService>()
    val referenceTableService = mockk<ReferenceTableService>()
    val restZaaktypeConfigurationConverter = mockk<RestZaaktypeConfigurationConverter>()
    val zaaktypeConfigurationService = mockk<ZaaktypeConfigurationService>()
    val processDefinitionConverter = mockk<RestProcessDefinitionConverter>()
    val smartDocumentsTemplatesService = mockk<SmartDocumentsTemplatesService>()
    val policyService = mockk<PolicyService>()
    val identityService = mockk<IdentityService>()
    val zaaktypeConfigurationRestService = ZaaktypeConfigurationRestService(
        ztcClientService = ztcClientService,
        configurationService = configurationService,
        zaaktypeConfigurationService = zaaktypeConfigurationService,
        zaaktypeConfigurationBeheerService = zaaktypeConfigurationBeheerService,
        referenceTableService = referenceTableService,
        restZaaktypeConfigurationConverter = restZaaktypeConfigurationConverter,
        processDefinitionConverter = processDefinitionConverter,
        smartDocumentsTemplatesService = smartDocumentsTemplatesService,
        policyService = policyService,
        identityService = identityService,
    )

    afterEach {
        checkUnnecessaryStub()
    }

    context("Zaakafhandelparameters without an ID (indicating new zaakafhandelparameters)") {
        given("productaanvraagtype that is not already in use by another zaaktype") {
            val productaanvraagtype = "fakeProductaanvraagtype"
            val restZaakafhandelParameters = createRestZaaktypeConfiguration(
                id = null,
                productaanvraagtype = productaanvraagtype
            )
            val zaakafhandelParameters = createZaaktypeCmmnConfiguration(
                id = null
            )
            val createdZaakafhandelParameters = createZaaktypeCmmnConfiguration(
                id = 1234L
            )
            val updatedRestZaakafhandelParameters = createRestZaaktypeConfiguration(
                id = 1234L,
                productaanvraagtype = productaanvraagtype
            )
            every { policyService.readOverigeRechten().canBeheren } returns true
            every {
                restZaaktypeConfigurationConverter.toZaaktypeConfiguration(restZaakafhandelParameters)
            } returns zaakafhandelParameters
            every {
                zaaktypeConfigurationBeheerService.checkProductaanvraagtypeIsNotInUse(
                    productaanvraagtype,
                    updatedRestZaakafhandelParameters.zaaktype.omschrijving!!
                )
            } just runs
            every {
                zaaktypeConfigurationBeheerService.storeConfiguration(zaakafhandelParameters)
            } returns createdZaakafhandelParameters
            every {
                restZaaktypeConfigurationConverter.toRestZaaktypeConfiguration(createdZaakafhandelParameters, true)
            } returns updatedRestZaakafhandelParameters

            `when`("the zaakafhandelparameters are created") {
                val returnedRestZaakafhandelParameters =
                    zaaktypeConfigurationRestService.createOrUpdateZaaktypeConfiguration(
                        restZaakafhandelParameters.zaaktype.uuid,
                        restZaakafhandelParameters
                    )

                then(
                    """
                the zaakafhandelparameters should be created
                """
                ) {
                    returnedRestZaakafhandelParameters shouldBe updatedRestZaakafhandelParameters
                    verify(exactly = 1) {
                        zaaktypeConfigurationBeheerService.storeConfiguration(zaakafhandelParameters)
                    }
                }
            }
        }
        given("productaanvraagtype that is already in use by another zaaktype") {
            val productaanvraagtype = "fakeProductaanvraagtype"
            val restZaakafhandelParameters = createRestZaaktypeConfiguration(
                id = null,
                productaanvraagtype = productaanvraagtype,
                restZaaktypeOverzicht = createRestZaaktypeOverzicht(omschrijving = "fakeZaaktypeOmschrijving2")
            )
            val zaakafhandelParameters = createZaaktypeCmmnConfiguration(
                id = null
            )
            every { policyService.readOverigeRechten().canBeheren } returns true
            every {
                zaaktypeConfigurationBeheerService.checkProductaanvraagtypeIsNotInUse(
                    productaanvraagtype,
                    restZaakafhandelParameters.zaaktype.omschrijving!!
                )
            } throws InputValidationFailedException(ERROR_CODE_PRODUCTAANVRAAGTYPE_ALREADY_IN_USE)

            `when`("the zaakafhandelparameters are created") {
                val exception = shouldThrow<InputValidationFailedException> {
                    zaaktypeConfigurationRestService.createOrUpdateZaaktypeConfiguration(
                        restZaakafhandelParameters.zaaktype.uuid,
                        restZaakafhandelParameters
                    )
                }

                then(
                    """
                an exception should be thrown indicating that the provided productaanvraagtype is already in use
                """
                ) {
                    exception.errorCode shouldBe ERROR_CODE_PRODUCTAANVRAAGTYPE_ALREADY_IN_USE
                    exception.message shouldBe null
                    verify(exactly = 0) {
                        zaaktypeConfigurationBeheerService.storeConfiguration(zaakafhandelParameters)
                    }
                }
            }
        }
    }

    given("SmartDocuments is disabled and empty set of templates is returned") {
        every { policyService.readOverigeRechten().canBeheren } returns true
        every { smartDocumentsTemplatesService.listTemplates() } returns emptySet()

        `when`("storing templates mapping") {
            val exception = shouldThrow<SmartDocumentsConfigurationException> {
                zaaktypeConfigurationRestService.storeSmartDocumentsTemplatesMapping(
                    UUID.randomUUID(),
                    emptySet()
                )
            }

            then("exception is thrown") {
                exception.message shouldBe "Validation failed. No SmartDocuments templates available"
            }
        }
    }

    given("A behandelaar is set but the behandelaar is not part of the behandelaar group") {
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(id = null)
        val behandelaarId = "fakeBehandelaarId"
        val behandelaarGroupId = "fakeBehandelaarGroupId"
        every { policyService.readOverigeRechten().canBeheren } returns true
        every {
            identityService.validateIfUserIsInGroup(behandelaarId, behandelaarGroupId)
        } throws UserNotInGroupException()

        `when`("zaaktypeCmmnConfiguration are created") {
            val restZaakafhandelParameters = createRestZaaktypeConfiguration(
                defaultBehandelaarId = behandelaarId,
                defaultGroupId = behandelaarGroupId
            )
            val exception = shouldThrow<InputValidationFailedException> {
                zaaktypeConfigurationRestService.createOrUpdateZaaktypeConfiguration(
                    restZaakafhandelParameters.zaaktype.uuid,
                    restZaakafhandelParameters
                )
            }

            then("an exception is thrown") {
                exception.errorCode shouldBe ERROR_CODE_USER_NOT_IN_GROUP
                exception.message shouldBe null
                verify(exactly = 0) {
                    zaaktypeConfigurationBeheerService.storeConfiguration(zaaktypeCmmnConfiguration)
                }
            }
        }
    }

    given("a configuration for another zaaktype version than the one in the path") {
        val restZaaktypeConfiguration = createRestZaaktypeConfiguration()
        val otherZaaktypeUuid = UUID.randomUUID()
        every { policyService.readOverigeRechten().canBeheren } returns true

        `when`("it is stored") {
            val exception = shouldThrow<InputValidationFailedException> {
                zaaktypeConfigurationRestService.createOrUpdateZaaktypeConfiguration(
                    otherZaaktypeUuid,
                    restZaaktypeConfiguration
                )
            }

            then("it is rejected and nothing is stored") {
                exception.errorCode shouldBe ERROR_CODE_VALIDATION_GENERIC
                verify(exactly = 0) { zaaktypeConfigurationBeheerService.storeConfiguration(any()) }
            }
        }
    }

    given("a configuration that names no process definition") {
        val restZaaktypeConfiguration = createRestZaaktypeConfiguration(restProcessDefinition = null)
        every { policyService.readOverigeRechten().canBeheren } returns true

        `when`("it is stored") {
            val exception = shouldThrow<InputValidationFailedException> {
                zaaktypeConfigurationRestService.createOrUpdateZaaktypeConfiguration(
                    restZaaktypeConfiguration.zaaktype.uuid,
                    restZaaktypeConfiguration
                )
            }

            then("it is rejected and nothing is stored") {
                exception.errorCode shouldBe ERROR_CODE_VALIDATION_GENERIC
                verify(exactly = 0) { zaaktypeConfigurationBeheerService.storeConfiguration(any()) }
            }
        }
    }

    createZaaktypeConfigurationsUnderTest().forEach { (configurationType, createZaaktypeConfiguration) ->
        given("an existing zaaktype configuration bound to $configurationType") {
            val zaaktypeConfiguration = createZaaktypeConfiguration("fakeNietOntvankelijkResultaattype")
            every { policyService.readOverigeRechten().canBeheren } returns true
            every {
                zaaktypeConfigurationService.findConfiguration(zaaktypeConfiguration.zaaktypeUuid)
            } returns zaaktypeConfiguration
            every {
                restZaaktypeConfigurationConverter.toRestZaaktypeConfiguration(zaaktypeConfiguration, true)
            } returns createRestZaaktypeConfiguration()

            `when`("the zaaktype configuration is requested") {
                zaaktypeConfigurationRestService.readZaaktypeConfiguration(zaaktypeConfiguration.zaaktypeUuid)

                then("the configuration is converted with its related data") {
                    verify(exactly = 1) {
                        restZaaktypeConfigurationConverter.toRestZaaktypeConfiguration(zaaktypeConfiguration, true)
                    }
                }
            }
        }
    }

    given("No existing zaaktype configuration") {
        val zaaktypeUuid = UUID.randomUUID()
        val newZaaktypeConfiguration = slot<ZaaktypeConfiguration>()
        every { policyService.readOverigeRechten().canBeheren } returns true
        every { zaaktypeConfigurationService.findConfiguration(zaaktypeUuid) } returns null
        every {
            restZaaktypeConfigurationConverter.toRestZaaktypeConfiguration(capture(newZaaktypeConfiguration), true)
        } returns createRestZaaktypeConfiguration()

        `when`("zaaktypeConfiguration is requested") {
            zaaktypeConfigurationRestService.readZaaktypeConfiguration(zaaktypeUuid)

            then("a new, unbound configuration for the zaaktype version is converted, so that a beheerder can configure it") {
                with(newZaaktypeConfiguration.captured) {
                    this.zaaktypeUuid shouldBe zaaktypeUuid
                    id shouldBe null
                    processBinding shouldBe null
                }
            }
        }
    }

    given("A configured AFZENDER system reference table with one reply-to address") {
        val referenceTable = createReferenceTable(
            code = AFZENDER.name,
            isSystemReferenceTable = true,
            values = mutableListOf(createReferenceTableValue(name = "fakeReplyToAddress", sortOrder = 0))
        )
        every { referenceTableService.readSystemReferenceTable(AFZENDER) } returns referenceTable
        every { referenceTableService.listReferenceTableValuesSorted(referenceTable) } returns referenceTable.values

        `when`("the reply-tos are listed") {
            val replyTos = zaaktypeConfigurationRestService.listReplyTos()

            then("the configured address is combined with the two special mail options, sorted") {
                replyTos.map { it.mail } shouldBe listOf("GEMEENTE", "MEDEWERKER", "fakeReplyToAddress")
                replyTos.map { it.isSpeciaal } shouldBe listOf(true, true, false)
            }
        }
    }

    context("listing endpoints") {
        given("the process definitions of both process engines") {
            val processDefinitions = listOf(
                createRestProcessDefinition(processEngine = ProcessEngine.CMMN, key = "fakeCaseDefinitionKey"),
                createRestProcessDefinition(processEngine = ProcessEngine.BPMN, key = "fakeProcessDefinitionKey")
            )
            every { policyService.readOverigeRechten().canBeheren } returns true
            every { processDefinitionConverter.listProcessDefinitions() } returns processDefinitions

            `when`("a beheerder lists the process definitions") {
                val returnedProcessDefinitions = zaaktypeConfigurationRestService.listBindableProcessDefinitions()

                then("those of both engines are returned") {
                    returnedProcessDefinitions shouldBe processDefinitions
                }
            }
        }

        given("catalogus and zaaktype configurations") {
            val catalogusUri = URI("https://example.com/catalogi/1")
            val zaakType = createZaakType()
            val config = ZaaktypeConfiguration().apply {
                zaaktypeUuid = zaakType.url.extractUuid()
            }
            val restConfig = createRestZaaktypeConfiguration(
                restZaaktypeOverzicht = createRestZaaktypeOverzicht(uuid = zaakType.url.extractUuid())
            )
            every { policyService.readOverigeRechten().canBeheren } returns true
            every { configurationService.readDefaultCatalogusURI() } returns catalogusUri
            every { ztcClientService.listZaaktypen(catalogusUri) } returns listOf(zaakType)
            every { zaaktypeConfigurationService.findConfiguration(zaakType.url.extractUuid()) } returns config
            every { restZaaktypeConfigurationConverter.toRestZaaktypeConfiguration(config, false) } returns restConfig

            `when`("listing zaaktype configurations") {
                val list = zaaktypeConfigurationRestService.listZaaktypeConfigurations()

                then("the converted configuration list is returned") {
                    list shouldHaveSize 1
                    list.first() shouldBe restConfig
                }
            }
        }

        given("zaakbeeindig redenen in configuration service") {
            val reden = ZaakbeeindigReden().apply {
                id = 1L
                naam = "fakeReden"
            }
            every { policyService.readOverigeRechten().canBeheren } returns true
            every { zaaktypeConfigurationService.listZaakbeeindigRedenen() } returns listOf(reden)

            `when`("listing all zaakbeeindig redenen") {
                val reasons = zaaktypeConfigurationRestService.listZaakbeeindigRedenen()

                then("reasons are converted and returned") {
                    reasons shouldHaveSize 1
                    reasons.first().naam shouldBe "fakeReden"
                }
            }
        }

        given("resultaattypes for a zaaktype") {
            val zaakType = createZaakType()
            val resultaatType = createResultaatType()
            every { policyService.readOverigeRechten().canBeheren } returns true
            every { ztcClientService.readZaaktype(zaakType.url.extractUuid()) } returns zaakType
            every { ztcClientService.readResultaattypen(zaakType.url) } returns listOf(resultaatType)

            `when`("listing resultaattypes for zaaktype for admins") {
                val resultaatTypes = zaaktypeConfigurationRestService.listResultaattypesForZaaktypeForAdmins(
                    zaakType.url.extractUuid()
                )

                then("resultaattypes are returned") {
                    resultaatTypes shouldHaveSize 1
                    resultaatTypes.first().naam shouldBe resultaatType.omschrijving
                }
            }
        }
    }
})
