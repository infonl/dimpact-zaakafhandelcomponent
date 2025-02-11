package net.atos.zac.flowable

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.ztc.model.createZaakType
import net.atos.zac.admin.model.createZaakafhandelParameters
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.flowable.cmmn.CMMNService
import org.flowable.cmmn.api.CmmnRepositoryService
import org.flowable.cmmn.api.CmmnRuntimeService
import org.flowable.cmmn.api.runtime.CaseInstance
import org.flowable.cmmn.api.runtime.CaseInstanceBuilder
import java.net.URI
import java.util.UUID

class CMMNServiceTest : BehaviorSpec({
    val cmmnRuntimeService = mockk<CmmnRuntimeService>()
    val cmmnRepositoryService = mockk<CmmnRepositoryService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val cmmnService = CMMNService(
        cmmnRuntimeService,
        cmmnRepositoryService,
        loggedInUserInstance
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A zaak and zaakafhandelparameters for the related zaaktype") {
        val zaakTypeUUID = UUID.randomUUID()
        val zaakUUID = UUID.randomUUID()
        val zaakType = createZaakType(
            uri = URI("https://example.com/zaaktypes/$zaakTypeUUID"),
        )
        val zaak = createZaak(
            zaakTypeURI = zaakType.url,
            uuid = zaakUUID
        )
        val zaakafhandelparameters = createZaakafhandelParameters(
            zaaktypeUUID = zaakTypeUUID
        )
        val zaakData = mapOf("dummyKey" to "dummyValue")
        val caseInstanceBuilder = mockk<CaseInstanceBuilder>()
        val caseInstance = mockk<CaseInstance>()

        every { cmmnRuntimeService.createCaseInstanceBuilder() } returns caseInstanceBuilder
        every {
            caseInstanceBuilder
                .caseDefinitionKey(zaakafhandelparameters.caseDefinitionID)
                .businessKey(zaakUUID.toString())
                .variable("zaakUUID", zaak.uuid)
                .variable("zaakIdentificatie", zaak.identificatie)
                .variable("zaaktypeUUID", zaakTypeUUID)
                .variable("zaaktypeOmschrijving", zaakType.omschrijving)
        } returns caseInstanceBuilder
        every { caseInstanceBuilder.variables(zaakData) } returns caseInstanceBuilder
        every { caseInstanceBuilder.start() } returns caseInstance

        When("the zaak is started using the CMMN service") {
            cmmnService.startCase(zaak, zaakType, zaakafhandelparameters, zaakData)

            Then("it is successfully started") {
                verify(exactly = 1) {
                    caseInstanceBuilder.start()
                }
            }
        }
    }
    Given("A CMMN case which has been started for a certain zaak") {
        val zaakUUID = UUID.randomUUID()
        val caseInstanceID = "dummyCaseInstanceID"
        val caseInstance = mockk<CaseInstance>()
        every {
            cmmnRuntimeService.createCaseInstanceQuery()
                .variableValueEquals(ZaakVariabelenService.VAR_ZAAK_UUID, zaakUUID)
                .singleResult()
        } returns caseInstance
        every { caseInstance.id } returns caseInstanceID
        every { cmmnRuntimeService.terminateCaseInstance(caseInstanceID) } just Runs

        When("the case is requested to be terminated") {
            cmmnService.terminateCase(zaakUUID)

            Then("it is successfully terminated") {
                verify(exactly = 1) {
                    cmmnRuntimeService.terminateCaseInstance(caseInstanceID)
                }
            }
        }
    }
})
