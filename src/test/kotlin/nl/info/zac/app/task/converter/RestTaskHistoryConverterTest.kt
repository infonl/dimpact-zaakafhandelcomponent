/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.app.task.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.mockk
import nl.info.test.org.flowable.task.service.impl.persistence.entity.createHistoricTaskLogEntryEntityImpl
import nl.info.zac.identity.IdentityService
import nl.info.zac.task.TaskHistoryService
import org.flowable.task.api.history.HistoricTaskLogEntryType
import org.flowable.task.service.impl.persistence.entity.HistoricTaskLogEntryEntityImpl

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
})
