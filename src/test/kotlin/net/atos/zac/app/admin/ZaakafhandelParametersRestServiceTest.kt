package net.atos.zac.app.admin

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

    Given("Zaakafhandelparameters with an ID (indicating existing zaakafhandelparameters)") {
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
            val returnedRestZaakafhandelParameters = zaakafhandelParametersRestService.updateZaakafhandelparameters(
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
})
