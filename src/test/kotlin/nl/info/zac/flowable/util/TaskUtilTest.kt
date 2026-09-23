/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.flowable.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import nl.info.zac.app.task.model.TaakStatus
import org.flowable.common.engine.api.scope.ScopeTypes
import org.flowable.task.api.Task
import org.flowable.task.api.TaskInfo

class TaskUtilTest : BehaviorSpec({

    given("an assigned, running Task") {
        val task = mockk<Task>()
        every { task.assignee } returns "fakeAssigneeId"

        `when`("taakStatus is read") {
            val status = task.taakStatus()

            then("it is TOEGEKEND") {
                status shouldBe TaakStatus.TOEGEKEND
            }
        }

        `when`("isOpen is read") {
            then("it is true") {
                task.isOpen() shouldBe true
            }
        }
    }

    given("an unassigned, running Task") {
        val task = mockk<Task>()
        every { task.assignee } returns null

        `when`("taakStatus is read") {
            val status = task.taakStatus()

            then("it is NIET_TOEGEKEND") {
                status shouldBe TaakStatus.NIET_TOEGEKEND
            }
        }

        `when`("isOpen is read") {
            then("it is true") {
                task.isOpen() shouldBe true
            }
        }
    }

    given("a completed task, represented as a plain TaskInfo rather than a Task") {
        val taskInfo = mockk<TaskInfo>()

        `when`("taakStatus is read") {
            val status = taskInfo.taakStatus()

            then("it is AFGEROND, regardless of who it was assigned to") {
                status shouldBe TaakStatus.AFGEROND
            }
        }

        `when`("isOpen is read") {
            then("it is false") {
                taskInfo.isOpen() shouldBe false
            }
        }
    }

    given("a task with a CMMN scope") {
        val taskInfo = mockk<TaskInfo>()
        every { taskInfo.scopeType } returns ScopeTypes.CMMN

        `when`("isCmmnTask is read") {
            then("it is true") {
                taskInfo.isCmmnTask() shouldBe true
            }
        }
    }

    given("a task without a CMMN scope") {
        val taskInfo = mockk<TaskInfo>()
        every { taskInfo.scopeType } returns null

        `when`("isCmmnTask is read") {
            then("it is false") {
                taskInfo.isCmmnTask() shouldBe false
            }
        }
    }
})
