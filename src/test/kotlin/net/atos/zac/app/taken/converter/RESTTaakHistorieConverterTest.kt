package net.atos.zac.app.taken.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.inspectors.forOne
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import org.flowable.task.api.history.HistoricTaskLogEntryType
import org.flowable.task.service.impl.persistence.entity.HistoricTaskLogEntryEntityImpl

class RESTTaakHistorieConverterTest : BehaviorSpec({

    Given("A converter history with USER_TASK_CREATED item") {
        val converter = RESTTaakHistorieConverter()
        val history = listOf(
            HistoricTaskLogEntryEntityImpl().apply {
                this.type = HistoricTaskLogEntryType.USER_TASK_CREATED.toString()
            }
        )

        When("convert is called") {
            val historieRegel = converter.convert(history)

            Then("it returns correct history lines") {
                historieRegel.forOne { line ->
                    line.attribuutLabel shouldBe RESTTaakHistorieConverter.STATUS_ATTRIBUUT_LABEL
                    line.oudeWaarde shouldBe null
                    line.nieuweWaarde shouldBe RESTTaakHistorieConverter.CREATED_ATTRIBUUT_LABEL
                    line.toelichting shouldBe null
                }
            }
        }
    }

    Given("A converter history with USER_TASK_COMPLETED item") {
        val converter = RESTTaakHistorieConverter()
        val history = listOf(
            HistoricTaskLogEntryEntityImpl().apply {
                this.type = HistoricTaskLogEntryType.USER_TASK_COMPLETED.toString()
            }
        )

        When("convert is called") {
            val historieRegel = converter.convert(history)

            Then("it returns correct history lines") {
                historieRegel.forOne { line ->
                    line.attribuutLabel shouldBe RESTTaakHistorieConverter.STATUS_ATTRIBUUT_LABEL
                    line.oudeWaarde shouldBe RESTTaakHistorieConverter.CREATED_ATTRIBUUT_LABEL
                    line.nieuweWaarde shouldBe RESTTaakHistorieConverter.COMPLETED_ATTRIBUUT_LABEL
                    line.toelichting shouldBe null
                }
            }
        }
    }

    Given("A converter history with non-supported item") {
        val converter = RESTTaakHistorieConverter()
        val history = listOf(
            HistoricTaskLogEntryEntityImpl().apply {
                this.type = HistoricTaskLogEntryType.USER_TASK_IDENTITY_LINK_ADDED.toString()
            }
        )

        When("convert is called") {
            val historieRegel = converter.convert(history)

            Then("it filters out the unsupported events") {
                historieRegel.shouldBeEmpty()
            }
        }
    }
})
