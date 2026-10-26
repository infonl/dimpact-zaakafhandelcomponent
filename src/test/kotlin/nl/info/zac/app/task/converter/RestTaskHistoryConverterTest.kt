/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.task.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.test.org.flowable.task.service.impl.persistence.entity.createHistoricTaskLogEntryEntityImpl
import nl.info.zac.flowable.task.FlowableTaskService
import nl.info.zac.history.model.toValue
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.createUser
import nl.info.zac.task.TaskHistoryService
import nl.info.zac.util.time.convertToLocalDate
import org.flowable.task.api.history.HistoricTaskLogEntryType
import org.flowable.task.service.impl.persistence.entity.HistoricTaskLogEntryEntityImpl
import java.util.Date

class RestTaskHistoryConverterTest : BehaviorSpec({
    val identityService = mockk<IdentityService>()
    val restTaskHistoryConverter = RestTaskHistoryConverter(identityService)

    afterEach {
        checkUnnecessaryStub()
    }

    given("A converter history with USER_TASK_CREATED item") {
        val history = listOf(createHistoricTaskLogEntryEntityImpl())

        `when`("convert is called") {
            val historieRegels = restTaskHistoryConverter.convert(history)

            then("it returns correct history lines") {
                historieRegels.first().let { line ->
                    line.attribuutLabel shouldBe RestTaskHistoryConverter.STATUS_ATTRIBUUT_LABEL
                    line.oudeWaarde shouldBe null
                    line.nieuweWaarde shouldBe RestTaskHistoryConverter.CREATED_ATTRIBUUT_LABEL
                    line.toelichting shouldBe null
                }
            }
        }
    }

    given("A converter history with USER_TASK_COMPLETED item") {
        val history = listOf(
            createHistoricTaskLogEntryEntityImpl(
                type = HistoricTaskLogEntryType.USER_TASK_COMPLETED
            )
        )

        `when`("convert is called") {
            val historieRegel = restTaskHistoryConverter.convert(history)

            then("it returns correct history lines") {
                historieRegel.first().let { line ->
                    line.attribuutLabel shouldBe RestTaskHistoryConverter.STATUS_ATTRIBUUT_LABEL
                    line.oudeWaarde shouldBe RestTaskHistoryConverter.CREATED_ATTRIBUUT_LABEL
                    line.nieuweWaarde shouldBe RestTaskHistoryConverter.COMPLETED_ATTRIBUUT_LABEL
                    line.toelichting shouldBe null
                }
            }
        }
    }

    given("A converter history with non-supported item") {
        val history = listOf(
            createHistoricTaskLogEntryEntityImpl(
                type = HistoricTaskLogEntryType.USER_TASK_IDENTITY_LINK_ADDED
            )
        )

        `when`("convert is called") {
            val historieRegel = restTaskHistoryConverter.convert(history)

            then("it filters out the unsupported events") {
                historieRegel.shouldBeEmpty()
            }
        }
    }

    given("A converter history with an item recording that a zaakspecifiek geautoriseerde medewerker was added") {
        val history = listOf(
            HistoricTaskLogEntryEntityImpl().apply {
                type = TaskHistoryService.USER_TASK_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER_ADDED
                data = "{\"explanation\":\"fakeExplanation\",\"newValue\":\"fakeFullName\",\"oldValue\":\"\"}"
            }
        )

        `when`("convert is called") {
            val historieRegels = restTaskHistoryConverter.convert(history)

            then("the line is labelled with the roltype omschrijving, exactly as the zaakhistorie shows it") {
                historieRegels.first().let { line ->
                    line.attribuutLabel shouldBe "Zaakspecifiek geautoriseerde medewerker"
                    line.oudeWaarde shouldBe ""
                    line.nieuweWaarde shouldBe "fakeFullName"
                    line.toelichting shouldBe "fakeExplanation"
                }
            }
        }
    }

    given("A converter history with an item recording that the task description was changed") {
        val history = listOf(
            HistoricTaskLogEntryEntityImpl().apply {
                type = FlowableTaskService.USER_TASK_DESCRIPTION_CHANGED
                data = "{\"explanation\":\"fakeExplanation\",\"newValue\":\"fakeNewValue\",\"oldValue\":\"fakeOldValue\"}"
            }
        )

        `when`("convert is called") {
            val historieRegels = restTaskHistoryConverter.convert(history)

            then("the line contains the old and new description and the explanation") {
                historieRegels.first().let { line ->
                    line.attribuutLabel shouldBe RestTaskHistoryConverter.TOELICHTING_ATTRIBUUT_LABEL
                    line.oudeWaarde shouldBe "fakeOldValue"
                    line.nieuweWaarde shouldBe "fakeNewValue"
                    line.toelichting shouldBe "fakeExplanation"
                }
            }
        }
    }

    given("A converter history with a USER_TASK_OWNER_CHANGED item") {
        val history = listOf(
            createHistoricTaskLogEntryEntityImpl(
                type = HistoricTaskLogEntryType.USER_TASK_OWNER_CHANGED,
                data = "{\"newAssigneeId\":\"fakeNewUsername\",\"previousAssigneeId\":\"fakePreviousUsername\"}"
            )
        )
        every {
            identityService.readUser(username = "fakePreviousUsername", logContext = any())
        } returns createUser(username = "fakePreviousUsername", fullName = "fakePreviousFullName")
        every {
            identityService.readUser(username = "fakeNewUsername", logContext = any())
        } returns createUser(username = "fakeNewUsername", fullName = "fakeNewFullName")

        `when`("convert is called") {
            val historieRegels = restTaskHistoryConverter.convert(history)

            then("the line contains the full names of the previous and new owner") {
                historieRegels.first().let { line ->
                    line.attribuutLabel shouldBe RestTaskHistoryConverter.AANGEMAAKT_DOOR_ATTRIBUUT_LABEL
                    line.oudeWaarde shouldBe "fakePreviousFullName"
                    line.nieuweWaarde shouldBe "fakeNewFullName"
                    line.toelichting shouldBe null
                }
            }
        }
    }

    given("A converter history with a USER_TASK_DUEDATE_CHANGED item") {
        val previousDueDateMillis = 1710504000000L
        val newDueDateMillis = 1711108800000L
        val history = listOf(
            createHistoricTaskLogEntryEntityImpl(
                type = HistoricTaskLogEntryType.USER_TASK_DUEDATE_CHANGED,
                data = "{\"newDueDate\":$newDueDateMillis,\"previousDueDate\":$previousDueDateMillis}"
            )
        )

        `when`("convert is called") {
            val historieRegels = restTaskHistoryConverter.convert(history)

            then("the line contains the previous and new due dates") {
                historieRegels.first().let { line ->
                    line.attribuutLabel shouldBe RestTaskHistoryConverter.FATALEDATUM_ATTRIBUUT_LABEL
                    line.oudeWaarde shouldBe convertToLocalDate(Date(previousDueDateMillis)).toValue()
                    line.nieuweWaarde shouldBe convertToLocalDate(Date(newDueDateMillis)).toValue()
                    line.toelichting shouldBe null
                }
            }
        }
    }
})
