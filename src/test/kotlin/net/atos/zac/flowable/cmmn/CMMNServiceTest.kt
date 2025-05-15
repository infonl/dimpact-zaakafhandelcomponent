/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.cmmn

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.Runs
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import net.atos.zac.flowable.ZaakVariabelenService
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.admin.model.createZaakafhandelParameters
import nl.info.zac.authentication.LoggedInUser
import org.flowable.cmmn.api.CmmnHistoryService
import org.flowable.cmmn.api.CmmnRepositoryService
import org.flowable.cmmn.api.CmmnRuntimeService
import org.flowable.cmmn.api.runtime.CaseInstance
import org.flowable.cmmn.api.runtime.CaseInstanceBuilder
import java.net.URI
import java.util.UUID

class CMMNServiceTest : BehaviorSpec({
    val cmmnRuntimeService = mockk<CmmnRuntimeService>()
    val cmmnRepositoryService = mockk<CmmnRepositoryService>()
    val cmmnHistoryService = mockk<CmmnHistoryService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val cmmnService = CMMNService(
        cmmnRuntimeService,
        cmmnHistoryService,
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
        val zaakData = mapOf("fakeKey" to "fakeValue")
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
        val caseInstanceID = "fakeCaseInstanceID"
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
    Given("A CMMN case for a certain zaak UUID") {
        val zaakUUID = UUID.randomUUID()
        val caseInstanceID = "fakeCaseInstanceID"
        val caseInstance = mockk<CaseInstance>()
        every {
            cmmnRuntimeService.createCaseInstanceQuery()
                .variableValueEquals(ZaakVariabelenService.VAR_ZAAK_UUID, zaakUUID)
                .singleResult()
        } returns caseInstance
        every { caseInstance.id } returns caseInstanceID
        every { cmmnRuntimeService.deleteCaseInstance(caseInstanceID) } just Runs
        every { cmmnHistoryService.deleteHistoricCaseInstance(caseInstanceID) } just Runs

        When("the case is requested to be deleted") {
            cmmnService.deleteCase(zaakUUID)

            Then("the case is successfully deleted") {
                verify(exactly = 1) {
                    cmmnRuntimeService.deleteCaseInstance(caseInstanceID)
                    cmmnHistoryService.deleteHistoricCaseInstance(caseInstanceID)
                }
            }
        }
    }
})
