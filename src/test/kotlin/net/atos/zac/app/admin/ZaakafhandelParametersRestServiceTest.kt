package net.atos.zac.app.admin

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.every
import io.mockk.mockk
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.admin.ReferenceTableService
import net.atos.zac.admin.ZaakafhandelParameterBeheerService
import net.atos.zac.admin.ZaakafhandelParameterService
import net.atos.zac.app.admin.converter.RESTCaseDefinitionConverter
import net.atos.zac.app.admin.converter.RESTZaakafhandelParametersConverter
import net.atos.zac.app.zaak.converter.RESTResultaattypeConverter
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.flowable.CMMNService
import net.atos.zac.policy.PolicyService
import net.atos.zac.smartdocuments.SmartDocumentsTemplatesService

class ZaakafhandelParametersRestServiceTest : BehaviorSpec({
    val ztcClientService = mockk<ZtcClientService>()
    val configuratieService = mockk<ConfiguratieService>()
    val cmmnService = mockk<CMMNService>()
    val zaakafhandelParameterService = mockk<ZaakafhandelParameterService>()
    val zaakafhandelParameterBeheerService = mockk<ZaakafhandelParameterBeheerService>()
    val referenceTableService = mockk<ReferenceTableService>()
    val zaakafhandelParametersConverter = mockk<RESTZaakafhandelParametersConverter>()
    val caseDefinitionConverter = mockk<RESTCaseDefinitionConverter>()
    val resultaattypeConverter = mockk<RESTResultaattypeConverter>()
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

    Given("ZaakafhandelParametersRestService") {
        val restZaakafhandelParameters = createRestZaakAfhandelParameters()
        every { policyService.readOverigeRechten().beheren } returns true

        When("getZaakafhandelParameters") {
            val updatedRestZaakafhandelParameters = zaakafhandelParametersRestService.updateZaakafhandelparameters(
                restZaakafhandelParameters
            )

            Then("should return a list of zaakafhandel parameters") {
            }
        }
    }
})
