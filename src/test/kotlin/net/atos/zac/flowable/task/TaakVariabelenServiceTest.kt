/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.flowable.task

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.should
import io.kotest.matchers.string.contain
import io.mockk.every
import io.mockk.mockk
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.task.TaakVariabelenService.readZaakUUID
import net.atos.zac.flowable.task.TaakVariabelenService.readZaaktypeUUID
import org.flowable.common.engine.api.scope.ScopeTypes
import org.flowable.task.api.TaskInfo
import java.io.File
import java.util.UUID

class TaakVariabelenServiceTest : BehaviorSpec({
    val taskInfo = mockk<TaskInfo>()

    Given("Task with correct zaak UUID object") {
        val expectedUUID = UUID.fromString("e58ed763-928c-4155-bee9-fdbaaadc15f3")

        every { taskInfo.scopeType } returns ScopeTypes.CMMN
        every { taskInfo.caseVariables } returns mapOf(ZaakVariabelenService.VAR_ZAAK_UUID to expectedUUID)

        When("reading the zaak UUID") {
            val uuid = readZaakUUID(taskInfo)

            Then("it returns the right information") {
                uuid shouldBeEqual expectedUUID
            }
        }
    }

    Given("Task with zaak UUID as string") {
        val expectedUUID = "e58ed763-928c-4155-bee9-fdbaaadc15f3"

        every { taskInfo.scopeType } returns ScopeTypes.CMMN
        every { taskInfo.caseVariables } returns mapOf(ZaakVariabelenService.VAR_ZAAK_UUID to expectedUUID)

        When("reading the zaak UUID") {
            val exception = shouldThrow<ClassCastException> {
                readZaakUUID(taskInfo)
            }

            Then("it throws an exception") {
                exception.message should contain("java.lang.String cannot be cast to class java.util.UUID")
            }
        }
    }

    Given("Task with zaak UUID as unknown object") {
        val expectedUUID = File("e58ed763-928c-4155-bee9-fdbaaadc15f3")

        every { taskInfo.scopeType } returns ScopeTypes.CMMN
        every { taskInfo.caseVariables } returns mapOf(ZaakVariabelenService.VAR_ZAAK_UUID to expectedUUID)

        When("reading the zaak UUID") {
            val exception = shouldThrow<ClassCastException> {
                readZaakUUID(taskInfo)
            }

            Then("it throws an exception") {
                exception.message should contain("java.io.File cannot be cast to class java.util.UUID")
            }
        }
    }

    Given("Task with correct zaak type UUID object") {
        val expectedUUID = UUID.fromString("e58ed763-928c-4155-bee9-fdbaaadc15f3")

        every { taskInfo.scopeType } returns ScopeTypes.CMMN
        every { taskInfo.caseVariables } returns mapOf(ZaakVariabelenService.VAR_ZAAKTYPE_UUUID to expectedUUID)

        When("reading the zaak type UUID") {
            val uuid = readZaaktypeUUID(taskInfo)

            Then("it returns the right information") {
                uuid shouldBeEqual expectedUUID
            }
        }
    }

    Given("Task with zaak type UUID as string") {
        val expectedUUID = "e58ed763-928c-4155-bee9-fdbaaadc15f3"

        every { taskInfo.scopeType } returns ScopeTypes.CMMN
        every { taskInfo.caseVariables } returns mapOf(ZaakVariabelenService.VAR_ZAAKTYPE_UUUID to expectedUUID)

        When("reading the zaak type UUID") {
            val exception = shouldThrow<ClassCastException> {
                readZaaktypeUUID(taskInfo)
            }

            Then("it throws an exception") {
                exception.message should contain("java.lang.String cannot be cast to class java.util.UUID")
            }
        }
    }

    Given("Task with zaak type UUID as unknown object") {
        val expectedUUID = File("e58ed763-928c-4155-bee9-fdbaaadc15f3")

        every { taskInfo.scopeType } returns ScopeTypes.CMMN
        every { taskInfo.caseVariables } returns mapOf(ZaakVariabelenService.VAR_ZAAKTYPE_UUUID to expectedUUID)

        When("reading the zaak type UUID") {
            val exception = shouldThrow<ClassCastException> {
                readZaaktypeUUID(taskInfo)
            }

            Then("it throws an exception") {
                exception.message should contain("java.io.File cannot be cast to class java.util.UUID")
            }
        }
    }
})
