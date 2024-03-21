/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldBeJsonArray
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.config.ItestConfiguration
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_GROUP_A_DESCRIPTION
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_GROUP_A_ID
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAAK_CREATED
import org.json.JSONArray
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * This test assumes a zaak has been created in a previously run test.
 */
@Order(TEST_SPEC_ORDER_AFTER_ZAAK_CREATED)
class PlanItemsRESTServiceTest : BehaviorSpec() {
    companion object {
        const val FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE = "AANVULLENDE_INFORMATIE"
        const val HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM = "Aanvullende informatie"
        const val HUMAN_TASK_TYPE = "HUMAN_TASK"
    }

    private val logger = KotlinLogging.logger {}
    private val itestHttpClient = ItestHttpClient()
    private lateinit var humanTaskItemAanvullendeInformatieId: String

    init {
        Given("A zaak has been created") {
            When("the list human task plan items endpoint is called") {
                val response = itestHttpClient.performGetRequest(
                    "${ItestConfiguration.ZAC_API_URI}/planitems/zaak/$zaak1UUID/humanTaskPlanItems"
                )
                Then(
                    "the list of human task plan items for this zaak is returned and contains " +
                        "the task 'aanvullende informatie'"
                ) {
                    val responseBody = response.body!!.string()
                    logger.info { "Response: $responseBody" }
                    response.isSuccessful shouldBe true
                    responseBody.shouldBeJsonArray()
                    // the zaak is in the intake phase, so there should be only be one human task
                    // plan item: 'aanvullende informatie'
                    JSONArray(responseBody).length() shouldBe 1
                    with(JSONArray(responseBody)[0].toString()) {
                        shouldContainJsonKeyValue("actief", "true")
                        shouldContainJsonKeyValue("formulierDefinitie", FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE)
                        shouldContainJsonKeyValue("naam", HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM)
                        shouldContainJsonKeyValue("type", HUMAN_TASK_TYPE)
                        shouldContainJsonKeyValue("zaakUuid", zaak1UUID.toString())
                        shouldContainJsonKey("id")
                    }
                    humanTaskItemAanvullendeInformatieId = JSONArray(responseBody).getJSONObject(0).getString("id")
                }
            }
        }
        Given("A zaak has been created") {
            When("the get human task plan item endpoint is called for the task 'aanvullende informatie'") {
                val response = itestHttpClient.performGetRequest(
                    "${ItestConfiguration.ZAC_API_URI}" +
                        "/planitems/humanTaskPlanItem/$humanTaskItemAanvullendeInformatieId"
                )
                Then("the human task plan item data for this task is returned") {
                    val responseBody = response.body!!.string()
                    logger.info { "Response: $responseBody" }
                    response.isSuccessful shouldBe true
                    with(responseBody) {
                        shouldContainJsonKeyValue("actief", "true")
                        shouldContainJsonKeyValue("formulierDefinitie", FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE)
                        shouldContainJsonKeyValue("naam", HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM)
                        shouldContainJsonKeyValue("type", HUMAN_TASK_TYPE)
                        shouldContainJsonKeyValue("zaakUuid", zaak1UUID.toString())
                        shouldContainJsonKeyValue("id", humanTaskItemAanvullendeInformatieId)
                    }
                }
            }
        }
        Given("A zaak has been created") {
            When("the start human task plan items endpoint is called") {
                val fataleDatum = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val response = itestHttpClient.performJSONPostRequest(
                    url = "${ItestConfiguration.ZAC_API_URI}/planitems/doHumanTaskPlanItem",
                    requestBodyAsString = """{
                        "planItemInstanceId":"$humanTaskItemAanvullendeInformatieId",
                        "fataledatum":"$fataleDatum",
                        "taakStuurGegevens":{"sendMail":false},
                        "medewerker":null,"groep":{"id":"$TEST_GROUP_A_ID","naam":"$TEST_GROUP_A_DESCRIPTION"},
                        "taakdata":{}
                    }
                    """.trimIndent()
                )
                Then("a task is started for this zaak") {
                    val responseBody = response.body!!.string()
                    logger.info { "Response: $responseBody" }

                    response.isSuccessful shouldBe true
                }
            }
        }
    }
}
