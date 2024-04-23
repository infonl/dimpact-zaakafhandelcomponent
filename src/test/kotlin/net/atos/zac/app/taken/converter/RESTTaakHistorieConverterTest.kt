package net.atos.zac.app.taken.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import net.atos.zac.identity.IdentityService
import org.flowable.task.api.history.HistoricTaskLogEntryType
import org.flowable.task.service.impl.persistence.entity.createHistoricTaskLogEntryEntityImpl

class RESTTaakHistorieConverterTest : BehaviorSpec({
    val identityService = mockk<IdentityService>()
    val restTaakHistorieConverter = RESTTaakHistorieConverter(identityService)

    Given("A converter history with USER_TASK_CREATED item") {
        val history = listOf(createHistoricTaskLogEntryEntityImpl())

        When("convert is called") {
            val historieRegels = restTaakHistorieConverter.convert(history)

            Then("it returns correct history lines") {
                historieRegels.first()!!.let { line ->
                    line.attribuutLabel shouldBe RESTTaakHistorieConverter.STATUS_ATTRIBUUT_LABEL
                    line.oudeWaarde shouldBe null
                    line.nieuweWaarde shouldBe RESTTaakHistorieConverter.CREATED_ATTRIBUUT_LABEL
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
            val historieRegel = restTaakHistorieConverter.convert(history)

            Then("it returns correct history lines") {
                historieRegel.first().let { line ->
                    line.attribuutLabel shouldBe RESTTaakHistorieConverter.STATUS_ATTRIBUUT_LABEL
                    line.oudeWaarde shouldBe RESTTaakHistorieConverter.CREATED_ATTRIBUUT_LABEL
                    line.nieuweWaarde shouldBe RESTTaakHistorieConverter.COMPLETED_ATTRIBUUT_LABEL
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
            val historieRegel = restTaakHistorieConverter.convert(history)

            Then("it filters out the unsupported events") {
                historieRegel.shouldBeEmpty()
            }
        }
    }
})
