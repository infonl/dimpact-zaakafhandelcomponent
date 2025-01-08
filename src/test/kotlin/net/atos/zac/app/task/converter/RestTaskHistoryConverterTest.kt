/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.task.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.mockk
import net.atos.zac.identity.IdentityService
import nl.info.test.org.flowable.task.service.impl.persistence.entity.createHistoricTaskLogEntryEntityImpl
import org.flowable.task.api.history.HistoricTaskLogEntryType

class RestTaskHistoryConverterTest : BehaviorSpec({
    val identityService = mockk<IdentityService>()
    val restTaskHistoryConverter = RestTaskHistoryConverter(identityService)

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("A converter history with USER_TASK_CREATED item") {
        val history = listOf(createHistoricTaskLogEntryEntityImpl())

        When("convert is called") {
            val historieRegels = restTaskHistoryConverter.convert(history)

            Then("it returns correct history lines") {
                historieRegels.first().let { line ->
                    line.attribuutLabel shouldBe RestTaskHistoryConverter.STATUS_ATTRIBUUT_LABEL
                    line.oudeWaarde shouldBe null
                    line.nieuweWaarde shouldBe RestTaskHistoryConverter.CREATED_ATTRIBUUT_LABEL
                    line.toelichting shouldBe null
                }
            }
        }
    }

    Given("A converter history with USER_TASK_COMPLETED item") {
        val history = listOf(
            createHistoricTaskLogEntryEntityImpl(
                type = HistoricTaskLogEntryType.USER_TASK_COMPLETED
            )
        )

        When("convert is called") {
            val historieRegel = restTaskHistoryConverter.convert(history)

            Then("it returns correct history lines") {
                historieRegel.first().let { line ->
                    line.attribuutLabel shouldBe RestTaskHistoryConverter.STATUS_ATTRIBUUT_LABEL
                    line.oudeWaarde shouldBe RestTaskHistoryConverter.CREATED_ATTRIBUUT_LABEL
                    line.nieuweWaarde shouldBe RestTaskHistoryConverter.COMPLETED_ATTRIBUUT_LABEL
                    line.toelichting shouldBe null
                }
            }
        }
    }

    Given("A converter history with non-supported item") {
        val history = listOf(
            createHistoricTaskLogEntryEntityImpl(
                type = HistoricTaskLogEntryType.USER_TASK_IDENTITY_LINK_ADDED
            )
        )

        When("convert is called") {
            val historieRegel = restTaskHistoryConverter.convert(history)

            Then("it filters out the unsupported events") {
                historieRegel.shouldBeEmpty()
            }
        }
    }
})
