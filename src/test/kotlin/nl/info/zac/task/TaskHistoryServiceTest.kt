/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.task

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import nl.info.client.zgw.model.createZaak
import nl.info.test.org.flowable.task.api.createTestTask
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.createUser
import org.flowable.engine.HistoryService
import org.flowable.task.api.history.HistoricTaskLogEntryBuilder

class TaskHistoryServiceTest : BehaviorSpec({
    val historyService = mockk<HistoryService>()
    val identityService = mockk<IdentityService>()
    val taskHistoryService = TaskHistoryService(historyService, identityService)

    afterEach {
        checkUnnecessaryStub()
    }

    context("Recording that a taakbehandelaar was granted zaakspecifieke autorisatie") {
        given("a task of a zaak and a medewerker known in Keycloak") {
            val task = createTestTask(id = "fakeTaskId")
            val zaak = createZaak(identificatie = "fakeZaakIdentificatie")
            val historicTaskLogEntryBuilder = mockk<HistoricTaskLogEntryBuilder>()
            val dataSlot = slot<String>()
            every { identityService.readUser("fakeMedewerkerId") } returns createUser(
                id = "fakeMedewerkerId",
                fullName = "fakeFullName"
            )
            every { historyService.createHistoricTaskLogEntryBuilder(task) } returns historicTaskLogEntryBuilder
            every {
                historicTaskLogEntryBuilder.type(TaskHistoryService.USER_TASK_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER_ADDED)
            } returns historicTaskLogEntryBuilder
            every { historicTaskLogEntryBuilder.data(capture(dataSlot)) } returns historicTaskLogEntryBuilder
            every { historicTaskLogEntryBuilder.create() } just runs

            `when`("the grant is recorded in the history of the task") {
                taskHistoryService.addZaakspecifiekGeautoriseerdeMedewerkerAddedEntry(
                    task = task,
                    zaak = zaak,
                    medewerkerId = "fakeMedewerkerId"
                )

                then("a history entry with the medewerker's full name and the fixed toelichting is created") {
                    dataSlot.captured shouldBe
                        "{\"explanation\":\"Zaakspecifiek geautoriseerd medewerker van zaak fakeZaakIdentificatie\"," +
                        "\"newValue\":\"fakeFullName\",\"oldValue\":\"\"}"
                    verify(exactly = 1) { historicTaskLogEntryBuilder.create() }
                }
            }
        }
    }
})
