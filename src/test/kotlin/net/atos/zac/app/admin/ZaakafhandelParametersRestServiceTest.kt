/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.admin

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.admin.ReferenceTableService
import net.atos.zac.admin.ZaakafhandelParameterBeheerService
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.admin.model.createZaakafhandelParameters
import net.atos.zac.app.admin.converter.RESTCaseDefinitionConverter
import net.atos.zac.app.admin.converter.RestZaakafhandelParametersConverter
import net.atos.zac.app.exception.InputValidationFailedException
import net.atos.zac.app.zaak.converter.RestResultaattypeConverter
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.flowable.cmmn.CMMNService
import net.atos.zac.policy.PolicyService
import net.atos.zac.smartdocuments.SmartDocumentsTemplatesService

class ZaakafhandelParametersRestServiceTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val configuratieService = mockk<ConfiguratieService>()
    val cmmnService = mockk<CMMNService>()
    val zaakafhandelParameterService = mockk<ZaakafhandelParameterService>()
    val zaakafhandelParameterBeheerService = mockk<ZaakafhandelParameterBeheerService>()
    val referenceTableService = mockk<ReferenceTableService>()
    val zaakafhandelParametersConverter = mockk<RestZaakafhandelParametersConverter>()
    val caseDefinitionConverter = mockk<RESTCaseDefinitionConverter>()
    val resultaattypeConverter = mockk<RestResultaattypeConverter>()
    val smartDocumentsTemplatesService = mockk<SmartDocumentsTemplatesService>()
    val policyService = mockk<PolicyService>()

    val zaakafhandelParametersRestService = ZaakafhandelParametersRestService(
        ztcClientService = ztcClientService,
        configuratieService = configuratieService,
        cmmnService = cmmnService,
        zaakafhandelParameterService = zaakafhandelParameterService,
        zaakafhandelParameterBeheerService = zaakafhandelParameterBeheerService,
        referenceTableService = referenceTableService,
        zaakafhandelParametersConverter = zaakafhandelParametersConverter,
        caseDefinitionConverter = caseDefinitionConverter,
        resultaattypeConverter = resultaattypeConverter,
        smartDocumentsTemplatesService = smartDocumentsTemplatesService,
        policyService = policyService
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
        every { zaakafhandelParameterBeheerService.updateZaakafhandelParameters(zaakafhandelParameters) } returns
            updatedZaakafhandelParameters
        every {
            zaakafhandelParameterService.cacheRemoveZaakafhandelParameters(zaakafhandelParameters.zaakTypeUUID)
        } just Runs
        every { zaakafhandelParameterService.clearListCache() } returns "cache cleared"
        every {
            zaakafhandelParametersConverter.toRestZaakafhandelParameters(updatedZaakafhandelParameters, true)
        } returns updatedRestZaakafhandelParameters

        When("the zaakafhandelparameters are updated with a different domein") {
            val returnedRestZaakafhandelParameters = zaakafhandelParametersRestService.createOrUpdateZaakafhandelparameters(
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
                    zaakafhandelParameterBeheerService.updateZaakafhandelParameters(zaakafhandelParameters)
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
            zaakafhandelParameterBeheerService.findActiveZaakafhandelparametersByProductaanvraagtype(productaanvraagtype)
        } returns
            emptyList()
        every {
            zaakafhandelParameterBeheerService.createZaakafhandelParameters(zaakafhandelParameters)
        } returns createdZaakafhandelParameters
        every {
            zaakafhandelParametersConverter.toRestZaakafhandelParameters(createdZaakafhandelParameters, true)
        } returns updatedRestZaakafhandelParameters

        When("the zaakafhandelparameters are created") {
            val returnedRestZaakafhandelParameters = zaakafhandelParametersRestService.createOrUpdateZaakafhandelparameters(
                restZaakafhandelParameters
            )

            Then(
                """
                the zaakafhandelparameters should be created
                """
            ) {
                returnedRestZaakafhandelParameters shouldBe updatedRestZaakafhandelParameters
                verify(exactly = 1) {
                    zaakafhandelParameterBeheerService.createZaakafhandelParameters(zaakafhandelParameters)
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
            restZaaktypeOverzicht = createRESTZaaktypeOverzicht(omschrijving = "dummyZaaktypeOmschrijving2")
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
            zaakafhandelParameterBeheerService.findActiveZaakafhandelparametersByProductaanvraagtype(productaanvraagtype)
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
                exception.message shouldBe "msg.error.productaanvraagtype.already.in.use"
                verify(exactly = 0) {
                    zaakafhandelParameterBeheerService.createZaakafhandelParameters(zaakafhandelParameters)
                }
            }
        }
    }
})
