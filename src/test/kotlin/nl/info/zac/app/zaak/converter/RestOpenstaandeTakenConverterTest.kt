/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.zaak.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import net.atos.zac.flowable.task.FlowableTaskService
import nl.info.test.org.flowable.task.api.createTestTask
import java.util.UUID

class RestOpenstaandeTakenConverterTest : BehaviorSpec({
    val flowableTaskService = mockk<FlowableTaskService>()
    val restOpenstaandeTakenConverter = RestOpenstaandeTakenConverter(flowableTaskService)

    afterEach {
        checkUnnecessaryStub()
    }

    context("convert") {
        given("a zaak UUID with two open tasks") {
            val zaakUUID = UUID.randomUUID()
            val task1 = createTestTask(name = "fakeTaskName1")
            val task2 = createTestTask(name = "fakeTaskName2")
            every { flowableTaskService.listOpenTasksForZaak(zaakUUID) } returns listOf(task1, task2)

            `when`("convert is called") {
                val result = restOpenstaandeTakenConverter.convert(zaakUUID)

                then("it returns the correct count and task names") {
                    result.aantalOpenstaandeTaken shouldBe 2
                    result.taakNamen shouldBe listOf("fakeTaskName1", "fakeTaskName2")
                }
            }
        }

        given("a zaak UUID with no open tasks") {
            val zaakUUID = UUID.randomUUID()
            every { flowableTaskService.listOpenTasksForZaak(zaakUUID) } returns emptyList()

            `when`("convert is called") {
                val result = restOpenstaandeTakenConverter.convert(zaakUUID)

                then("it returns zero count and empty task names") {
                    result.aantalOpenstaandeTaken shouldBe 0
                    result.taakNamen shouldBe emptyList()
                }
            }
        }
    }
})
