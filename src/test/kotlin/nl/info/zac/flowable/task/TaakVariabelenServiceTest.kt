/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.flowable.task

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.contain
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.exception.VariableNotFoundException
import org.flowable.engine.TaskService
import org.flowable.task.api.Task
import org.flowable.common.engine.api.scope.ScopeTypes
import org.flowable.task.api.TaskInfo
import java.io.File
import java.util.UUID

class TaakVariabelenServiceTest : BehaviorSpec({
    val taskInfo = mockk<TaskInfo>()

    given("Task with correct zaak UUID object") {
        val expectedUUID = UUID.fromString("e58ed763-928c-4155-bee9-fdbaaadc15f3")

        every { taskInfo.scopeType } returns ScopeTypes.CMMN
        every { taskInfo.caseVariables } returns mapOf(ZaakVariabelenService.VAR_ZAAK_UUID to expectedUUID)

        `when`("reading the zaak UUID") {
            val uuid = readZaakUUID(taskInfo)

            then("it returns the right information") {
                uuid shouldBeEqual expectedUUID
            }
        }
    }

    given("Task with zaak UUID as string") {
        val expectedUUID = "e58ed763-928c-4155-bee9-fdbaaadc15f3"

        every { taskInfo.scopeType } returns ScopeTypes.CMMN
        every { taskInfo.caseVariables } returns mapOf(ZaakVariabelenService.VAR_ZAAK_UUID to expectedUUID)

        `when`("reading the zaak UUID") {
            val exception = shouldThrow<ClassCastException> {
                readZaakUUID(taskInfo)
            }

            then("it throws an exception") {
                exception.message should contain("java.lang.String cannot be cast to class java.util.UUID")
            }
        }
    }

    given("Task with zaak UUID as unknown object") {
        val expectedUUID = File("e58ed763-928c-4155-bee9-fdbaaadc15f3")

        every { taskInfo.scopeType } returns ScopeTypes.CMMN
        every { taskInfo.caseVariables } returns mapOf(ZaakVariabelenService.VAR_ZAAK_UUID to expectedUUID)

        `when`("reading the zaak UUID") {
            val exception = shouldThrow<ClassCastException> {
                readZaakUUID(taskInfo)
            }

            then("it throws an exception") {
                exception.message should contain("java.io.File cannot be cast to class java.util.UUID")
            }
        }
    }

    given("Task with correct zaak type UUID object") {
        val expectedUUID = UUID.fromString("e58ed763-928c-4155-bee9-fdbaaadc15f3")

        every { taskInfo.scopeType } returns ScopeTypes.CMMN
        every { taskInfo.caseVariables } returns mapOf(ZaakVariabelenService.VAR_ZAAKTYPE_UUID to expectedUUID)

        `when`("reading the zaak type UUID") {
            val uuid = readZaaktypeUUID(taskInfo)

            then("it returns the right information") {
                uuid shouldBeEqual expectedUUID
            }
        }
    }

    given("Task with zaak type UUID as string") {
        val expectedUUID = "e58ed763-928c-4155-bee9-fdbaaadc15f3"

        every { taskInfo.scopeType } returns ScopeTypes.CMMN
        every { taskInfo.caseVariables } returns mapOf(ZaakVariabelenService.VAR_ZAAKTYPE_UUID to expectedUUID)

        `when`("reading the zaak type UUID") {
            val exception = shouldThrow<ClassCastException> {
                readZaaktypeUUID(taskInfo)
            }

            then("it throws an exception") {
                exception.message should contain("java.lang.String cannot be cast to class java.util.UUID")
            }
        }
    }

    given("Task with zaak type UUID as unknown object") {
        val expectedUUID = File("e58ed763-928c-4155-bee9-fdbaaadc15f3")

        every { taskInfo.scopeType } returns ScopeTypes.CMMN
        every { taskInfo.caseVariables } returns mapOf(ZaakVariabelenService.VAR_ZAAKTYPE_UUID to expectedUUID)

        `when`("reading the zaak type UUID") {
            val exception = shouldThrow<ClassCastException> {
                readZaaktypeUUID(taskInfo)
            }

            then("it throws an exception") {
                exception.message should contain("java.io.File cannot be cast to class java.util.UUID")
            }
        }
    }

    given("A BPMN task with a zaak identificatie process variable") {
        every { taskInfo.scopeType } returns ScopeTypes.BPMN
        every { taskInfo.processVariables } returns mapOf(ZaakVariabelenService.VAR_ZAAK_IDENTIFICATIE to "fakeZaakIdentificatie")

        `when`("reading the zaak identificatie") {
            val zaakIdentificatie = readZaakIdentificatie(taskInfo)

            then("it returns the process variable") {
                zaakIdentificatie shouldBe "fakeZaakIdentificatie"
            }
        }
    }

    given("A CMMN task without a zaaktype omschrijving case variable") {
        every { taskInfo.scopeType } returns ScopeTypes.CMMN
        every { taskInfo.caseVariables } returns emptyMap()
        every { taskInfo.name } returns "fakeTaskName"
        every { taskInfo.id } returns "fakeTaskId"

        `when`("reading the zaaktype omschrijving") {
            val variableNotFoundException = shouldThrow<VariableNotFoundException> {
                readZaaktypeOmschrijving(taskInfo)
            }

            then("it throws an exception that names the variable and the task") {
                variableNotFoundException.message shouldBe
                    "No variable found with name 'zaaktypeOmschrijving' for task with name 'fakeTaskName' and id 'fakeTaskId'"
            }
        }
    }

    given("A task without task local variables") {
        every { taskInfo.taskLocalVariables } returns emptyMap()

        `when`("reading the task data, task information and task documents") {
            val taskData = readTaskData(taskInfo)
            val taskInformation = readTaskInformation(taskInfo)
            val taskDocuments = readTaskDocuments(taskInfo)

            then("they are all empty") {
                taskData shouldBe emptyMap()
                taskInformation shouldBe emptyMap()
                taskDocuments shouldBe emptyList()
            }
        }
    }

    given("A task with task data, task information and task documents as task local variables") {
        val documentUUID = UUID.randomUUID()
        every { taskInfo.taskLocalVariables } returns mapOf(
            "taakdata" to mapOf("fakeKey" to "fakeValue"),
            "taakinformatie" to mapOf("fakeInformationKey" to "fakeInformationValue"),
            "taakdocumenten" to listOf(documentUUID)
        )

        `when`("reading the task data, task information and task documents") {
            val taskData = readTaskData(taskInfo)
            val taskInformation = readTaskInformation(taskInfo)
            val taskDocuments = readTaskDocuments(taskInfo)

            then("they return the task local variables") {
                taskData shouldBe mapOf("fakeKey" to "fakeValue")
                taskInformation shouldBe mapOf("fakeInformationKey" to "fakeInformationValue")
                taskDocuments shouldBe listOf(documentUUID)
            }
        }
    }

    given("Task data with mail fields of which some are empty") {
        val taakData = mapOf(
            "verzender" to "fakeFrom@example.com",
            "replyTo" to "",
            "emailadres" to "fakeTo@example.com",
            "body" to "fakeBody"
        )

        `when`("reading the mail fields") {
            then("the filled fields are returned and the empty or missing fields are null") {
                readMailFrom(taakData) shouldBe "fakeFrom@example.com"
                readMailReplyTo(taakData).shouldBeNull()
                readMailTo(taakData) shouldBe "fakeTo@example.com"
                readMailBody(taakData) shouldBe "fakeBody"
                readMailAttachments(taakData).shouldBeNull()
            }
        }
    }

    given("Mutable task data with a mail body") {
        `when`("setting a new mail body") {
            val taakData = mutableMapOf("body" to "fakeOldBody")
            setMailBody(taakData, "fakeNewBody")

            then("the mail body is replaced") {
                taakData shouldBe mapOf("body" to "fakeNewBody")
            }
        }

        `when`("setting the mail body to null") {
            val taakData = mutableMapOf("body" to "fakeOldBody", "fakeKey" to "fakeValue")
            setMailBody(taakData, null)

            then("the mail body is removed") {
                taakData shouldBe mapOf("fakeKey" to "fakeValue")
            }
        }
    }

    given("Task data with string flags set to 'true'") {
        val taakData = mapOf(
            "zaakOpschorten" to "true",
            "zaakHervatten" to "true",
            "ondertekenen" to "fakeDocumentUUID1;fakeDocumentUUID2"
        )

        `when`("reading the flags and values") {
            then("the flags are true and the values are returned") {
                isZaakOpschorten(taakData) shouldBe true
                isZaakHervatten(taakData) shouldBe true
                readSignatures(taakData) shouldBe "fakeDocumentUUID1;fakeDocumentUUID2"
            }
        }
    }

    given("Task data with flags that are not the string 'true'") {
        val taakData = mapOf<String, Any>(
            "zaakHervatten" to true,
            "ondertekenen" to listOf("fakeDocumentUUID")
        )

        `when`("reading the flags and values") {
            then("the flags are false and non-string values are ignored") {
                isZaakOpschorten(mapOf("zaakOpschorten" to "TRUE")) shouldBe false
                isZaakHervatten(taakData) shouldBe false
                readSignatures(taakData).shouldBeNull()
            }
        }
    }

    given("A task") {
        val taskService = mockk<TaskService>()
        val taakVariabelenService = TaakVariabelenService(taskService)
        val task = mockk<Task>()
        val taakdata = mapOf<String, Any>("fakeKey" to "fakeValue")
        val taakinformatie = mapOf("fakeInformationKey" to "fakeInformationValue")
        val taakdocumenten = listOf(UUID.randomUUID())
        every { task.id } returns "fakeTaskId"
        every { taskService.setVariableLocal("fakeTaskId", any(), any()) } just runs

        `when`("setting the task data, task information and task documents") {
            taakVariabelenService.setTaskData(task, taakdata)
            taakVariabelenService.setTaskInformation(task, taakinformatie)
            taakVariabelenService.setTaskDocuments(task, taakdocumenten)

            then("they are stored as task local variables") {
                verify(exactly = 1) {
                    taskService.setVariableLocal("fakeTaskId", "taakdata", taakdata)
                    taskService.setVariableLocal("fakeTaskId", "taakinformatie", taakinformatie)
                    taskService.setVariableLocal("fakeTaskId", "taakdocumenten", taakdocumenten)
                }
            }
        }
    }
})
