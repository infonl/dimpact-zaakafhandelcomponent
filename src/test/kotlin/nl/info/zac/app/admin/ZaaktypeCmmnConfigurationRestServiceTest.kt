/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.admin

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import net.atos.zac.admin.ZaaktypeCmmnConfigurationService
import net.atos.zac.app.admin.converter.RESTCaseDefinitionConverter
import net.atos.zac.flowable.cmmn.CMMNService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.admin.ReferenceTableService
import nl.info.zac.admin.ZaaktypeBpmnConfigurationBeheerService
import nl.info.zac.admin.ZaaktypeBpmnConfigurationService
import nl.info.zac.admin.ZaaktypeCmmnConfigurationBeheerService
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.app.admin.converter.RestZaakafhandelParametersConverter
import nl.info.zac.app.admin.model.createRestZaakafhandelParameters
import nl.info.zac.app.admin.model.createRestZaaktypeOverzicht
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.exception.ErrorCode.ERROR_CODE_PRODUCTAANVRAAGTYPE_ALREADY_IN_USE
import nl.info.zac.exception.ErrorCode.ERROR_CODE_USER_NOT_IN_GROUP
import nl.info.zac.exception.InputValidationFailedException
import nl.info.zac.flowable.bpmn.model.createZaaktypeBpmnConfiguration
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.exception.UserNotInGroupException
import nl.info.zac.policy.PolicyService
import nl.info.zac.smartdocuments.SmartDocumentsTemplatesService
import nl.info.zac.smartdocuments.exception.SmartDocumentsConfigurationException
import java.util.UUID

class ZaaktypeCmmnConfigurationRestServiceTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val configuratieService = mockk<ConfiguratieService>()
    val cmmnService = mockk<CMMNService>()
    val zaaktypeCmmnConfigurationService = mockk<ZaaktypeCmmnConfigurationService>()
    val zaaktypeCmmnConfigurationBeheerService = mockk<ZaaktypeCmmnConfigurationBeheerService>()
    val referenceTableService = mockk<ReferenceTableService>()
    val zaaktypeCmmnConfigurationConverter = mockk<RestZaakafhandelParametersConverter>()
    val zaaktypeBpmnConfigurationService = mockk<ZaaktypeBpmnConfigurationService>()
    val zaaktypeBpmnConfigurationBeheerService = mockk<ZaaktypeBpmnConfigurationBeheerService>()
    val zaaktypeConfigurationService = mockk<ZaaktypeConfigurationService>()
    val caseDefinitionConverter = mockk<RESTCaseDefinitionConverter>()
    val smartDocumentsTemplatesService = mockk<SmartDocumentsTemplatesService>()
    val policyService = mockk<PolicyService>()
    val identityService = mockk<IdentityService>()
    val zaaktypeCmmnConfigurationRestService = ZaaktypeCmmnConfigurationRestService(
        ztcClientService = ztcClientService,
        configuratieService = configuratieService,
        cmmnService = cmmnService,
        zaaktypeCmmnConfigurationService = zaaktypeCmmnConfigurationService,
        zaaktypeCmmnConfigurationBeheerService = zaaktypeCmmnConfigurationBeheerService,
        zaaktypeBpmnConfigurationService = zaaktypeBpmnConfigurationService,
        zaaktypeBpmnConfigurationBeheerService = zaaktypeBpmnConfigurationBeheerService,
        zaaktypeConfigurationService = zaaktypeConfigurationService,
        referenceTableService = referenceTableService,
        zaaktypeCmmnConfigurationConverter = zaaktypeCmmnConfigurationConverter,
        caseDefinitionConverter = caseDefinitionConverter,
        smartDocumentsTemplatesService = smartDocumentsTemplatesService,
        policyService = policyService,
        identityService = identityService,
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Context(" Zaakafhandelparameters with an ID (indicating existing zaakafhandelparameters)") {
        Given("no productaanvraagtype") {
            val initialDomein = "initialDomein"
            val updatedDomein = "updatedDomein"
            val restZaakafhandelParameters = createRestZaakafhandelParameters(domein = initialDomein)
            val updatedRestZaakafhandelParameters = createRestZaakafhandelParameters(domein = updatedDomein)
            val zaakafhandelParameters = createZaaktypeCmmnConfiguration(
                id = 1234L,
                domein = initialDomein
            )
            val updatedZaakafhandelParameters = createZaaktypeCmmnConfiguration(
                id = 1234L,
                domein = updatedDomein
            )
            every { policyService.readOverigeRechten().beheren } returns true
            every {
                zaaktypeCmmnConfigurationConverter.toZaaktypeCmmnConfiguration(restZaakafhandelParameters)
            } returns zaakafhandelParameters
            every { zaaktypeCmmnConfigurationBeheerService.storeZaaktypeCmmnConfiguration(zaakafhandelParameters) } returns
                updatedZaakafhandelParameters
            every {
                zaaktypeCmmnConfigurationService.cacheRemoveZaaktypeCmmnConfiguration(zaakafhandelParameters.zaaktypeUuid)
            } just runs
            every { zaaktypeCmmnConfigurationService.clearListCache() } returns "cache cleared"
            every {
                zaaktypeCmmnConfigurationConverter.toRestZaaktypeCmmnConfiguration(updatedZaakafhandelParameters, true)
            } returns updatedRestZaakafhandelParameters

            When("the zaakafhandelparameters are updated with a different domein") {
                val returnedRestZaakafhandelParameters =
                    zaaktypeCmmnConfigurationRestService.createOrUpdateZaaktypeCmmnConfiguration(
                        restZaakafhandelParameters
                    )

                Then(
                    """
                the zaakafhandelparameters should be updated and both the zaakafhandelparameters read cache as well as the 
                zaakafhandelparameters list cache should be updated
                """
                ) {
                    returnedRestZaakafhandelParameters shouldBe updatedRestZaakafhandelParameters
                    verify(exactly = 1) {
                        zaaktypeCmmnConfigurationBeheerService.storeZaaktypeCmmnConfiguration(zaakafhandelParameters)
                        zaaktypeCmmnConfigurationService.cacheRemoveZaaktypeCmmnConfiguration(
                            zaakafhandelParameters.zaaktypeUuid
                        )
                        zaaktypeCmmnConfigurationService.clearListCache()
                    }
                }
            }
        }
    }

    Context("Zaakafhandelparameters without an ID (indicating new zaakafhandelparameters)") {
        Given("productaanvraagtype that is not already in use by another zaaktype") {
            val productaanvraagtype = "fakeProductaanvraagtype"
            val restZaakafhandelParameters = createRestZaakafhandelParameters(
                id = null,
                productaanvraagtype = productaanvraagtype
            )
            val zaakafhandelParameters = createZaaktypeCmmnConfiguration(
                id = null
            )
            val createdZaakafhandelParameters = createZaaktypeCmmnConfiguration(
                id = 1234L
            )
            val updatedRestZaakafhandelParameters = createRestZaakafhandelParameters(
                id = 1234L,
                productaanvraagtype = productaanvraagtype
            )
            every { policyService.readOverigeRechten().beheren } returns true
            every {
                zaaktypeCmmnConfigurationConverter.toZaaktypeCmmnConfiguration(restZaakafhandelParameters)
            } returns zaakafhandelParameters
            every {
                zaaktypeCmmnConfigurationBeheerService.checkIfProductaanvraagtypeIsNotAlreadyInUse(
                    productaanvraagtype, updatedRestZaakafhandelParameters.zaaktype.omschrijving!!
                )
            } just runs
            every {
                zaaktypeBpmnConfigurationService.checkIfProductaanvraagtypeIsNotAlreadyInUse(productaanvraagtype)
            } just runs
            every {
                zaaktypeCmmnConfigurationBeheerService.storeZaaktypeCmmnConfiguration(zaakafhandelParameters)
            } returns createdZaakafhandelParameters
            every {
                zaaktypeCmmnConfigurationConverter.toRestZaaktypeCmmnConfiguration(createdZaakafhandelParameters, true)
            } returns updatedRestZaakafhandelParameters
            every {
                zaaktypeCmmnConfigurationService.cacheRemoveZaaktypeCmmnConfiguration(zaakafhandelParameters.zaaktypeUuid)
            } just runs
            every { zaaktypeCmmnConfigurationService.clearListCache() } returns "cache cleared"

            When("the zaakafhandelparameters are created") {
                val returnedRestZaakafhandelParameters =
                    zaaktypeCmmnConfigurationRestService.createOrUpdateZaaktypeCmmnConfiguration(
                        restZaakafhandelParameters
                    )

                Then(
                    """
                the zaakafhandelparameters should be created
                """
                ) {
                    returnedRestZaakafhandelParameters shouldBe updatedRestZaakafhandelParameters
                    verify(exactly = 1) {
                        zaaktypeCmmnConfigurationBeheerService.storeZaaktypeCmmnConfiguration(zaakafhandelParameters)
                    }
                }
            }
        }
        Given("productaanvraagtype that is already in use by another zaaktype") {
            val productaanvraagtype = "fakeProductaanvraagtype"
            val restZaakafhandelParameters = createRestZaakafhandelParameters(
                id = null,
                productaanvraagtype = productaanvraagtype,
                restZaaktypeOverzicht = createRestZaaktypeOverzicht(omschrijving = "fakeZaaktypeOmschrijving2")
            )
            val zaakafhandelParameters = createZaaktypeCmmnConfiguration(
                id = null
            )
            every { policyService.readOverigeRechten().beheren } returns true
            every {
                zaaktypeCmmnConfigurationBeheerService.checkIfProductaanvraagtypeIsNotAlreadyInUse(
                    productaanvraagtype, restZaakafhandelParameters.zaaktype.omschrijving!!
                )
            } throws InputValidationFailedException(ERROR_CODE_PRODUCTAANVRAAGTYPE_ALREADY_IN_USE)

            When("the zaakafhandelparameters are created") {
                val exception = shouldThrow<InputValidationFailedException> {
                    zaaktypeCmmnConfigurationRestService.createOrUpdateZaaktypeCmmnConfiguration(
                        restZaakafhandelParameters
                    )
                }

                Then(
                    """
                an exception should be thrown indicating that the provided productaanvraagtype is already in use
                """
                ) {
                    exception.errorCode shouldBe ERROR_CODE_PRODUCTAANVRAAGTYPE_ALREADY_IN_USE
                    exception.message shouldBe null
                    verify(exactly = 0) {
                        zaaktypeCmmnConfigurationBeheerService.storeZaaktypeCmmnConfiguration(zaakafhandelParameters)
                    }
                }
            }
        }
    }

    Given("SmartDocuments is disabled and empty set of templates is returned") {
        every { policyService.readOverigeRechten().beheren } returns true
        every { smartDocumentsTemplatesService.listTemplates() } returns emptySet()

        When("storing templates mapping") {
            val exception = shouldThrow<SmartDocumentsConfigurationException> {
                zaaktypeCmmnConfigurationRestService.storeSmartDocumentsTemplatesMapping(
                    UUID.randomUUID(),
                    emptySet()
                )
            }

            Then("exception is thrown") {
                exception.message shouldBe "Validation failed. No SmartDocuments templates available"
            }
        }
    }

    Given("A behandelaar is set but the behandelaar is not part of the behandelaar group") {
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(id = null)
        val behandelaarId = "fakeBehandelaarId"
        val behandelaarGroupId = "fakeBehandelaarGroupId"
        every { policyService.readOverigeRechten().beheren } returns true
        every {
            identityService.validateIfUserIsInGroup(behandelaarId, behandelaarGroupId)
        } throws UserNotInGroupException()

        When("zaaktypeCmmnConfiguration are created") {
            val restZaakafhandelParameters = createRestZaakafhandelParameters(
                defaultBehandelaarId = behandelaarId,
                defaultGroupId = behandelaarGroupId
            )
            val exception = shouldThrow<InputValidationFailedException> {
                zaaktypeCmmnConfigurationRestService.createOrUpdateZaaktypeCmmnConfiguration(
                    restZaakafhandelParameters
                )
            }

            Then("an exception is thrown") {
                exception.errorCode shouldBe ERROR_CODE_USER_NOT_IN_GROUP
                exception.message shouldBe null
                verify(exactly = 0) {
                    zaaktypeCmmnConfigurationBeheerService.storeZaaktypeCmmnConfiguration(zaaktypeCmmnConfiguration)
                }
            }
        }
    }

    Given("Existing zaaktype configuration for CMMN") {
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(id = null)
        every { policyService.readOverigeRechten().beheren } returns true
        every {
            zaaktypeConfigurationService.readZaaktypeConfiguration(zaaktypeCmmnConfiguration.zaaktypeUuid)
        } returns zaaktypeCmmnConfiguration
        every {
            zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaaktypeCmmnConfiguration.zaaktypeUuid)
        } returns zaaktypeCmmnConfiguration
        every {
            zaaktypeCmmnConfigurationConverter.toRestZaaktypeCmmnConfiguration(zaaktypeCmmnConfiguration, true)
        } returns createRestZaakafhandelParameters()

        When("zaaktypeCmmnConfiguration is requested") {
            zaaktypeCmmnConfigurationRestService.readZaaktypeConfiguration(
                zaaktypeCmmnConfiguration.zaaktypeUuid
            )

            Then("the correct functions are called to retrieve the configuration") {
                verify(exactly = 1) {
                    zaaktypeConfigurationService.readZaaktypeConfiguration(zaaktypeCmmnConfiguration.zaaktypeUuid)
                    zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(
                        zaaktypeCmmnConfiguration.zaaktypeUuid
                    )
                    zaaktypeCmmnConfigurationConverter.toRestZaaktypeCmmnConfiguration(zaaktypeCmmnConfiguration, true)
                }
            }
        }
    }

    Given("Existing zaaktype configuration for BPMN") {
        val zaaktypeBpmnConfiguration = createZaaktypeBpmnConfiguration(id = null)
        every { policyService.readOverigeRechten().beheren } returns true
        every {
            zaaktypeConfigurationService.readZaaktypeConfiguration(zaaktypeBpmnConfiguration.zaaktypeUuid)
        } returns zaaktypeBpmnConfiguration
        every {
            zaaktypeBpmnConfigurationBeheerService.findConfiguration(zaaktypeBpmnConfiguration.zaaktypeUuid)
        } returns zaaktypeBpmnConfiguration
        every {
            zaaktypeCmmnConfigurationConverter.toRestZaaktypeBpmnConfiguration(zaaktypeBpmnConfiguration)
        } returns createRestZaakafhandelParameters()

        When("zaaktypeCmmnConfiguration is requested") {
            zaaktypeCmmnConfigurationRestService.readZaaktypeConfiguration(
                zaaktypeBpmnConfiguration.zaaktypeUuid
            )

            Then("the correct functions are called to retrieve the configuration") {
                verify(exactly = 1) {
                    zaaktypeConfigurationService.readZaaktypeConfiguration(zaaktypeBpmnConfiguration.zaaktypeUuid)
                    zaaktypeBpmnConfigurationBeheerService.findConfiguration(zaaktypeBpmnConfiguration.zaaktypeUuid)
                    zaaktypeCmmnConfigurationConverter.toRestZaaktypeBpmnConfiguration(zaaktypeBpmnConfiguration)
                }
            }
        }
    }

    Given("No existing zaaktype configuration") {
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(id = null)
        every { policyService.readOverigeRechten().beheren } returns true
        every {
            zaaktypeConfigurationService.readZaaktypeConfiguration(zaaktypeCmmnConfiguration.zaaktypeUuid)
        } returns null
        every {
            zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(zaaktypeCmmnConfiguration.zaaktypeUuid)
        } returns zaaktypeCmmnConfiguration
        every {
            zaaktypeCmmnConfigurationConverter.toRestZaaktypeCmmnConfiguration(zaaktypeCmmnConfiguration, true)
        } returns createRestZaakafhandelParameters()

        When("zaaktypeConfiguration is requested") {
            zaaktypeCmmnConfigurationRestService.readZaaktypeConfiguration(
                zaaktypeCmmnConfiguration.zaaktypeUuid
            )

            Then("the correct functions are called to retrieve the configuration") {
                verify(exactly = 1) {
                    zaaktypeConfigurationService.readZaaktypeConfiguration(zaaktypeCmmnConfiguration.zaaktypeUuid)
                    zaaktypeCmmnConfigurationService.readZaaktypeCmmnConfiguration(
                        zaaktypeCmmnConfiguration.zaaktypeUuid
                    )
                    zaaktypeCmmnConfigurationConverter.toRestZaaktypeCmmnConfiguration(zaaktypeCmmnConfiguration, true)
                }
            }
        }
    }
})
