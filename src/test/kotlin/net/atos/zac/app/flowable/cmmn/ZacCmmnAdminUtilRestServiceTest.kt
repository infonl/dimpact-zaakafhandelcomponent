/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.flowable.cmmn

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import net.atos.zac.policy.PolicyService
import nl.info.zac.app.admin.flowable.cmmn.ZacCmmnAdminUtilRestService
import org.flowable.cmmn.api.CmmnRuntimeService
import org.flowable.cmmn.api.CmmnTaskService
import org.flowable.engine.RuntimeService

class ZacCmmnAdminUtilRestServiceTest : BehaviorSpec({
    val cmmnRuntimeService = mockk<CmmnRuntimeService>()
    val cmmnTaskService = mockk<CmmnTaskService>()
    val runtimeService = mockk<RuntimeService>()
    val policyService = mockk<PolicyService>()
    val zakCmmnAdminUtilRestService = ZacCmmnAdminUtilRestService(
        cmmnRuntimeService = cmmnRuntimeService,
        cmmnTaskService = cmmnTaskService,
        runtimeService = runtimeService,
        policyService = policyService
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A user with 'beheren' permissions") {
        every { policyService.readOverigeRechten().beheren } returns true
        every { cmmnRuntimeService.createCaseInstanceQuery().variableNotExists(any()).count() } returns 123L

        When("countMissingVariables is called") {
            val response = zakCmmnAdminUtilRestService.countMissingVariables()

            Then("should return no content") {
                response.status shouldBe 204
            }
        }
    }
})
