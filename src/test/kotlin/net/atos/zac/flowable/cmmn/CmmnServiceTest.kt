/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable.cmmn

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
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
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.authentication.LoggedInUser
import org.flowable.cmmn.api.CmmnHistoryService
import org.flowable.cmmn.api.CmmnRepositoryService
import org.flowable.cmmn.api.CmmnRuntimeService
import org.flowable.cmmn.api.history.HistoricCaseInstance
import org.flowable.cmmn.api.runtime.CaseInstance
import org.flowable.cmmn.api.runtime.CaseInstanceBuilder
import java.net.URI
import java.util.UUID

class CmmnServiceTest : BehaviorSpec({
    val cmmnRuntimeService = mockk<CmmnRuntimeService>()
    val cmmnRepositoryService = mockk<CmmnRepositoryService>()
    val cmmnHistoryService = mockk<CmmnHistoryService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val cmmnService = CmmnService(
        cmmnRuntimeService,
        cmmnHistoryService,
        cmmnRepositoryService,
        loggedInUserInstance
    )

    afterEach {
        checkUnnecessaryStub()
    }

    given("A zaak and zaaktypeCmmnConfiguration for the related zaaktype") {
        val zaakTypeUUID = UUID.randomUUID()
        val zaakUUID = UUID.randomUUID()
        val zaakType = createZaakType(
            uri = URI("https://example.com/zaaktypes/$zaakTypeUUID"),
        )
        val zaak = createZaak(
            zaaktypeUri = zaakType.url,
            uuid = zaakUUID
        )
        val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration(
            zaaktypeUUID = zaakTypeUUID
        )
        val zaakData = mapOf("fakeKey" to "fakeValue")
        val caseInstanceBuilder = mockk<CaseInstanceBuilder>()
        val caseInstance = mockk<CaseInstance>()

        every { cmmnRuntimeService.createCaseInstanceBuilder() } returns caseInstanceBuilder
        every {
            caseInstanceBuilder
                .caseDefinitionKey(checkNotNull(zaaktypeCmmnConfiguration.processBinding).definitionKey)
                .businessKey(zaakUUID.toString())
                .variable("zaakUUID", zaak.uuid)
                .variable("zaakIdentificatie", zaak.identificatie)
                .variable("zaaktypeUUID", zaakTypeUUID)
                .variable("zaaktypeOmschrijving", zaakType.omschrijving)
        } returns caseInstanceBuilder
        every { caseInstanceBuilder.variables(zaakData) } returns caseInstanceBuilder
        every { caseInstanceBuilder.start() } returns caseInstance

        `when`("the zaak is started using the CMMN service") {
            cmmnService.startCase(
                zaak = zaak,
                zaaktype = zaakType,
                caseDefinitionKey = checkNotNull(zaaktypeCmmnConfiguration.processBinding).definitionKey,
                zaakData = zaakData
            )

            then("it is successfully started") {
                verify(exactly = 1) {
                    caseInstanceBuilder.start()
                }
            }
        }
    }
    given("A CMMN case which has been started for a certain zaak") {
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

        `when`("the case is requested to be terminated") {
            cmmnService.terminateCase(zaakUUID)

            then("it is successfully terminated") {
                verify(exactly = 1) {
                    cmmnRuntimeService.terminateCaseInstance(caseInstanceID)
                }
            }
        }
    }
    given("a zaak with a running case instance and the history of two case instances") {
        val zaakUUID = UUID.randomUUID()
        val caseInstance = mockk<CaseInstance>()
        val historicCaseInstance1 = mockk<HistoricCaseInstance>()
        val historicCaseInstance2 = mockk<HistoricCaseInstance>()
        every { caseInstance.id } returns "fakeCaseInstanceId"
        every { historicCaseInstance1.id } returns "fakeHistoricCaseInstanceId1"
        every { historicCaseInstance2.id } returns "fakeHistoricCaseInstanceId2"
        every {
            cmmnRuntimeService.createCaseInstanceQuery().caseInstanceBusinessKey(zaakUUID.toString()).singleResult()
        } returns caseInstance
        every { cmmnRuntimeService.deleteCaseInstance("fakeCaseInstanceId") } just Runs
        every {
            cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceBusinessKey(zaakUUID.toString()).list()
        } returns listOf(historicCaseInstance1, historicCaseInstance2)
        every { cmmnHistoryService.deleteHistoricCaseInstance(any()) } just Runs

        `when`("the case is deleted") {
            cmmnService.deleteCase(zaakUUID)

            then("the running case instance and the history of every case instance of the zaak are deleted") {
                verify(exactly = 1) {
                    cmmnRuntimeService.deleteCaseInstance("fakeCaseInstanceId")
                    cmmnHistoryService.deleteHistoricCaseInstance("fakeHistoricCaseInstanceId1")
                    cmmnHistoryService.deleteHistoricCaseInstance("fakeHistoricCaseInstanceId2")
                }
            }
        }
    }
    given("a zaak whose case instance has ended, so that only its history is left") {
        val zaakUUID = UUID.randomUUID()
        val historicCaseInstance = mockk<HistoricCaseInstance>()
        every { historicCaseInstance.id } returns "fakeHistoricCaseInstanceId"
        every {
            cmmnRuntimeService.createCaseInstanceQuery().caseInstanceBusinessKey(zaakUUID.toString()).singleResult()
        } returns null
        every {
            cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceBusinessKey(zaakUUID.toString()).list()
        } returns listOf(historicCaseInstance)
        every { cmmnHistoryService.deleteHistoricCaseInstance("fakeHistoricCaseInstanceId") } just Runs

        `when`("the case is deleted") {
            cmmnService.deleteCase(zaakUUID)

            then("the history of the ended case instance is deleted") {
                verify(exactly = 1) { cmmnHistoryService.deleteHistoricCaseInstance("fakeHistoricCaseInstanceId") }
                verify(exactly = 0) { cmmnRuntimeService.deleteCaseInstance(any()) }
            }
        }
    }
    given("a zaak with a running CMMN case instance") {
        val zaakUUID = UUID.randomUUID()
        val caseInstance = mockk<CaseInstance>()
        every {
            cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceBusinessKey(zaakUUID.toString())
                .singleResult()
        } returns caseInstance

        `when`("isZaakCaseDriven is called") {
            val isZaakCaseDriven = cmmnService.isZaakCaseDriven(zaakUUID)

            then("the zaak is reported as case driven") {
                isZaakCaseDriven shouldBe true
            }
        }
    }
    given("a zaak without a running CMMN case instance") {
        val zaakUUID = UUID.randomUUID()
        every {
            cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceBusinessKey(zaakUUID.toString())
                .singleResult()
        } returns null

        `when`("isZaakCaseDriven is called") {
            val isZaakCaseDriven = cmmnService.isZaakCaseDriven(zaakUUID)

            then("the zaak is not reported as case driven") {
                isZaakCaseDriven shouldBe false
            }
        }
    }
})
