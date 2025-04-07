/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.admin

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.app.admin.converter.RESTCaseDefinitionConverter
import net.atos.zac.flowable.cmmn.CMMNService
import net.atos.zac.policy.PolicyService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.admin.ReferenceTableService
import nl.info.zac.admin.ZaakafhandelParameterBeheerService
import nl.info.zac.admin.model.createZaakafhandelParameters
import nl.info.zac.app.admin.converter.RestZaakafhandelParametersConverter
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.exception.ErrorCode.ERROR_CODE_PRODUCTAANVRAAGTYPE_ALREADY_IN_USE
import nl.info.zac.exception.ErrorCode.ERROR_CODE_USER_NOT_IN_GROUP
import nl.info.zac.exception.InputValidationFailedException
import nl.info.zac.identity.IdentityService
import nl.info.zac.smartdocuments.SmartDocumentsTemplatesService
import nl.info.zac.smartdocuments.exception.SmartDocumentsConfigurationException
import org.keycloak.admin.client.resource.RealmResource
import org.keycloak.admin.client.resource.UserResource
import org.keycloak.admin.client.resource.UsersResource
import org.keycloak.representations.idm.GroupRepresentation
import org.keycloak.representations.idm.UserRepresentation
import java.util.UUID

class ZaakafhandelParametersRestServiceTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val configuratieService = mockk<ConfiguratieService>()
    val cmmnService = mockk<CMMNService>()
    val zaakafhandelParameterService = mockk<ZaakafhandelParameterService>()
    val zaakafhandelParameterBeheerService = mockk<ZaakafhandelParameterBeheerService>()
    val referenceTableService = mockk<ReferenceTableService>()
    val zaakafhandelParametersConverter = mockk<RestZaakafhandelParametersConverter>()
    val caseDefinitionConverter = mockk<RESTCaseDefinitionConverter>()
    val smartDocumentsTemplatesService = mockk<SmartDocumentsTemplatesService>()
    val policyService = mockk<PolicyService>()
    val realmResource = mockk<RealmResource>()
    val identityService = IdentityService(realmResource)

    val zaakafhandelParametersRestService = ZaakafhandelParametersRestService(
        ztcClientService = ztcClientService,
        configuratieService = configuratieService,
        cmmnService = cmmnService,
        zaakafhandelParameterService = zaakafhandelParameterService,
        zaakafhandelParameterBeheerService = zaakafhandelParameterBeheerService,
        referenceTableService = referenceTableService,
        zaakafhandelParametersConverter = zaakafhandelParametersConverter,
        caseDefinitionConverter = caseDefinitionConverter,
        smartDocumentsTemplatesService = smartDocumentsTemplatesService,
        policyService = policyService,
        identityService = identityService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given(
        """
            Zaakafhandelparameters with an ID (indicating existing zaakafhandelparameters) 
            and without a productaanvraagtype
            """
    ) {
        val initialDomein = "initialDomein"
        val updatedDomein = "updatedDomein"
        val restZaakafhandelParameters = createRestZaakAfhandelParameters(domein = initialDomein)
        val updatedRestZaakafhandelParameters = createRestZaakAfhandelParameters(domein = updatedDomein)
        val zaakafhandelParameters = createZaakafhandelParameters(
            id = 1234L,
            domein = initialDomein
        )
        val updatedZaakafhandelParameters = createZaakafhandelParameters(
            id = 1234L,
            domein = updatedDomein
        )
        every { policyService.readOverigeRechten().beheren } returns true
        every {
            zaakafhandelParametersConverter.toZaakafhandelParameters(restZaakafhandelParameters)
        } returns zaakafhandelParameters
        every { zaakafhandelParameterBeheerService.storeZaakafhandelParameters(zaakafhandelParameters) } returns
            updatedZaakafhandelParameters
        every {
            zaakafhandelParameterService.cacheRemoveZaakafhandelParameters(zaakafhandelParameters.zaakTypeUUID)
        } just Runs
        every { zaakafhandelParameterService.clearListCache() } returns "cache cleared"
        every {
            zaakafhandelParametersConverter.toRestZaakafhandelParameters(updatedZaakafhandelParameters, true)
        } returns updatedRestZaakafhandelParameters

        When("the zaakafhandelparameters are updated with a different domein") {
            val returnedRestZaakafhandelParameters =
                zaakafhandelParametersRestService.createOrUpdateZaakafhandelparameters(
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
                    zaakafhandelParameterBeheerService.storeZaakafhandelParameters(zaakafhandelParameters)
                    zaakafhandelParameterService.cacheRemoveZaakafhandelParameters(zaakafhandelParameters.zaakTypeUUID)
                    zaakafhandelParameterService.clearListCache()
                }
            }
        }
    }
    Given(
        """
            Zaakafhandelparameters without an ID (indicating new zaakafhandelparameters) 
            and with a productaanvraagtype that is not already in use by another zaaktype
            """
    ) {
        val productaanvraagtype = "dummyProductaanvraagtype"
        val restZaakafhandelParameters = createRestZaakAfhandelParameters(
            id = null,
            productaanvraagtype = productaanvraagtype
        )
        val zaakafhandelParameters = createZaakafhandelParameters(
            id = null
        )
        val createdZaakafhandelParameters = createZaakafhandelParameters(
            id = 1234L
        )
        val updatedRestZaakafhandelParameters = createRestZaakAfhandelParameters(
            id = 1234L,
            productaanvraagtype = productaanvraagtype
        )
        every { policyService.readOverigeRechten().beheren } returns true
        every {
            zaakafhandelParametersConverter.toZaakafhandelParameters(restZaakafhandelParameters)
        } returns zaakafhandelParameters
        every {
            zaakafhandelParameterBeheerService.findActiveZaakafhandelparametersByProductaanvraagtype(
                productaanvraagtype
            )
        } returns
            emptyList()
        every {
            zaakafhandelParameterBeheerService.storeZaakafhandelParameters(zaakafhandelParameters)
        } returns createdZaakafhandelParameters
        every {
            zaakafhandelParametersConverter.toRestZaakafhandelParameters(createdZaakafhandelParameters, true)
        } returns updatedRestZaakafhandelParameters

        When("the zaakafhandelparameters are created") {
            val returnedRestZaakafhandelParameters =
                zaakafhandelParametersRestService.createOrUpdateZaakafhandelparameters(
                    restZaakafhandelParameters
                )

            Then(
                """
                the zaakafhandelparameters should be created
                """
            ) {
                returnedRestZaakafhandelParameters shouldBe updatedRestZaakafhandelParameters
                verify(exactly = 1) {
                    zaakafhandelParameterBeheerService.storeZaakafhandelParameters(zaakafhandelParameters)
                }
            }
        }
    }
    Given(
        """
            Zaakafhandelparameters without an ID (indicating new zaakafhandelparameters) 
            and with a productaanvraagtype that is already in use by another zaaktype
            """
    ) {
        val productaanvraagtype = "dummyProductaanvraagtype"
        val restZaakafhandelParameters = createRestZaakAfhandelParameters(
            id = null,
            productaanvraagtype = productaanvraagtype,
            restZaaktypeOverzicht = createRestZaaktypeOverzicht(omschrijving = "dummyZaaktypeOmschrijving2")
        )
        val zaakafhandelParameters = createZaakafhandelParameters(
            id = null
        )
        val activeZaakafhandelParametersForThisProductaanvraagtype = createZaakafhandelParameters(
            id = 1234L,
            productaanvraagtype = productaanvraagtype,
            zaaktypeOmschrijving = "dummyZaaktypeOmschrijving1"
        )
        every { policyService.readOverigeRechten().beheren } returns true
        every {
            zaakafhandelParameterBeheerService.findActiveZaakafhandelparametersByProductaanvraagtype(
                productaanvraagtype
            )
        } returns
            listOf(activeZaakafhandelParametersForThisProductaanvraagtype)

        When("the zaakafhandelparameters are created") {
            val exception = shouldThrow<InputValidationFailedException> {
                zaakafhandelParametersRestService.createOrUpdateZaakafhandelparameters(
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
                    zaakafhandelParameterBeheerService.storeZaakafhandelParameters(zaakafhandelParameters)
                }
            }
        }
    }

    Given("SmartDocuments is disabled and empty set of templates is returned") {
        every { policyService.readOverigeRechten().beheren } returns true
        every { smartDocumentsTemplatesService.listTemplates() } returns emptySet()

        When("storing templates mapping") {
            val exception = shouldThrow<SmartDocumentsConfigurationException> {
                zaakafhandelParametersRestService.storeTemplatesMapping(
                    UUID.randomUUID(),
                    emptySet()
                )
            }

            Then("exception is thrown") {
                exception.message shouldBe "Validation failed. No SmartDocuments templates available"
            }
        }
    }

    Given("A behandelaar is set") {
        When("the behandelaar is not part of the behandelaar group") {
            every { policyService.readOverigeRechten().beheren } returns true

            val userId = "1"
            val userResources = mockk<UsersResource>()
            every { userResources.searchByUsername(any(), true) } returns listOf(
                mockk<UserRepresentation> {
                    every { id } returns userId
                }
            )
            every { userResources.get(userId) } returns mockk<UserResource> {
                every { groups() } returns emptyList<GroupRepresentation>()
            }
            every { realmResource.users() } returns userResources

            val restZaakafhandelParameters = createRestZaakAfhandelParameters(
                defaultBehandelaarId = "defaultBehandelaarId",
                defaultGroupId = "defaultGroupId"
            )
            val zaakafhandelParameters = createZaakafhandelParameters(
                id = null
            )

            val exception = shouldThrow<InputValidationFailedException> {
                zaakafhandelParametersRestService.createOrUpdateZaakafhandelparameters(
                    restZaakafhandelParameters
                )
            }

            Then("an exception is thrown") {
                exception.errorCode shouldBe ERROR_CODE_USER_NOT_IN_GROUP
                exception.message shouldBe null
                verify(exactly = 0) {
                    zaakafhandelParameterBeheerService.storeZaakafhandelParameters(zaakafhandelParameters)
                }
            }
        }
    }
})
