/*
 * SPDX-FileCopyrightText: 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.flowable

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.flowable.cmmn.api.CmmnHistoryService
import org.flowable.cmmn.api.CmmnRuntimeService
import org.flowable.cmmn.api.runtime.CaseInstance
import org.flowable.engine.HistoryService
import org.flowable.engine.RuntimeService
import java.util.UUID

class ZaakVariabelenServiceTest : BehaviorSpec({
    val cmmnRuntimeService = mockk<CmmnRuntimeService>()
    val cmmnHistoryService = mockk<CmmnHistoryService>()
    val bpmnRuntimeService = mockk<RuntimeService>()
    val bpmnHistoryService = mockk<HistoryService>()
    val zaaKVariabelenService = ZaakVariabelenService(
        cmmnRuntimeService,
        cmmnHistoryService,
        bpmnRuntimeService,
        bpmnHistoryService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A case instance with a zaak UUID case variable") {
        val zaakUUID = UUID.randomUUID()
        val planItemInstance = createTestPlanItemInstance()
        val caseInstance = mockk<CaseInstance>()
        val caseVariables = mapOf("zaakUUID" to zaakUUID)
        every {
            cmmnRuntimeService.createCaseInstanceQuery()
                .caseInstanceId(planItemInstance.getCaseInstanceId())
                .includeCaseVariables()
                .singleResult()
        } returns caseInstance
        every { caseInstance.caseVariables } returns caseVariables

        When("the zaak UUID variable is read") {
            val returnedZaakUUID = zaaKVariabelenService.readZaakUUID(planItemInstance)

            Then("the zaak UUID is correctly returned") {
                returnedZaakUUID shouldBe zaakUUID
                verify(exactly = 1) {
                    cmmnRuntimeService.createCaseInstanceQuery()
                }
            }
        }
    }
})
