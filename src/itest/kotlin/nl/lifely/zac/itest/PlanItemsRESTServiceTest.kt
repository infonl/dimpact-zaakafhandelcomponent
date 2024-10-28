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
import nl.lifely.zac.itest.client.ZacClient
import nl.lifely.zac.itest.config.ItestConfiguration.FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE
import nl.lifely.zac.itest.config.ItestConfiguration.HTTP_STATUS_OK
import nl.lifely.zac.itest.config.ItestConfiguration.HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_GROUP_A_DESCRIPTION
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_GROUP_A_ID
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAAK_CREATED
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_PRODUCTAANVRAAG_1_UITERLIJKE_EINDDATUM_AFDOENING
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.lifely.zac.itest.config.ItestConfiguration.zaakProductaanvraag1Uuid
import org.json.JSONArray
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * This test assumes a zaak has been created in a previously run test.
 */
@Order(TEST_SPEC_ORDER_AFTER_ZAAK_CREATED)
class PlanItemsRESTServiceTest : BehaviorSpec({
    val humanTaskType = "HUMAN_TASK"

    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()

    lateinit var humanTaskItemAanvullendeInformatieId: String

    Given("A zaak has been created") {
        When("the list human task plan items endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                "$ZAC_API_URI/planitems/zaak/$zaakProductaanvraag1Uuid/humanTaskPlanItems"
            )
            Then("the list of human task plan items for this zaak contains the task 'aanvullende informatie'") {
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
                    shouldContainJsonKeyValue("type", humanTaskType)
                    shouldContainJsonKeyValue("zaakUuid", zaakProductaanvraag1Uuid.toString())
                    shouldContainJsonKey("id")
                }
                humanTaskItemAanvullendeInformatieId = JSONArray(responseBody).getJSONObject(0).getString("id")
            }
        }

        When("the get human task plan item endpoint is called for the task 'aanvullende informatie'") {
            val response = itestHttpClient.performGetRequest(
                "$ZAC_API_URI/planitems/humanTaskPlanItem/$humanTaskItemAanvullendeInformatieId"
            )
            Then("the human task plan item data for this task is returned") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                with(responseBody) {
                    shouldContainJsonKeyValue("actief", "true")
                    shouldContainJsonKeyValue("formulierDefinitie", FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE)
                    shouldContainJsonKeyValue("naam", HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM)
                    shouldContainJsonKeyValue("type", humanTaskType)
                    shouldContainJsonKeyValue("zaakUuid", zaakProductaanvraag1Uuid.toString())
                    shouldContainJsonKeyValue("id", humanTaskItemAanvullendeInformatieId)
                }
            }
        }

        When("the start human task plan items endpoint is called") {
            val fataleDatum = LocalDate.parse(ZAAK_PRODUCTAANVRAAG_1_UITERLIJKE_EINDDATUM_AFDOENING)
                .minusDays(1)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val response = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/planitems/doHumanTaskPlanItem",
                requestBodyAsString = """{
                    "planItemInstanceId":"$humanTaskItemAanvullendeInformatieId",
                    "fataledatum":"$fataleDatum",
                    "taakStuurGegevens":{"sendMail":false},
                    "medewerker":null,"groep":{"id":"$TEST_GROUP_A_ID","naam":"$TEST_GROUP_A_DESCRIPTION"},
                    "taakdata":{}
                }""".trimIndent()
            )
            Then("a task is started for this zaak") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }

                response.isSuccessful shouldBe true
            }
        }

        When("creation of a new additional info task with fatal date past the zaak fatal date is requested") {
            val newAdditionalTaskInfoResponse = itestHttpClient.performGetRequest(
                "$ZAC_API_URI/planitems/zaak/$zaakProductaanvraag1Uuid/humanTaskPlanItems"
            )
            val newAdditionalTaskInfoResponseBody = newAdditionalTaskInfoResponse.body!!.string()
            logger.info { "Response: $newAdditionalTaskInfoResponseBody" }
            newAdditionalTaskInfoResponse.isSuccessful shouldBe true
            val newAdditionalInfoTaskId = JSONArray(newAdditionalTaskInfoResponseBody).getJSONObject(0).getString("id")

            val fataleDatum = LocalDate.parse(ZAAK_PRODUCTAANVRAAG_1_UITERLIJKE_EINDDATUM_AFDOENING)
                .plusDays(2)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val response = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/planitems/doHumanTaskPlanItem",
                requestBodyAsString = """{
                    "planItemInstanceId":"$newAdditionalInfoTaskId",
                    "fataledatum":"$fataleDatum",
                    "taakStuurGegevens":{"sendMail":false},
                    "medewerker":null,"groep":{"id":"$TEST_GROUP_A_ID","naam":"$TEST_GROUP_A_DESCRIPTION"},
                    "taakdata":{}
                }""".trimIndent()
            )

            Then("a new task is started for this zaak") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }

                response.isSuccessful shouldBe true
            }

            And("zaak fatal date is moved forward to correspond to the task fatal date") {
                val zacResponse = zacClient.retrieveZaak(zaakProductaanvraag1Uuid)
                val responseBody = zacResponse.body!!.string()
                logger.info { "Response: $responseBody" }

                with(zacResponse) {
                    code shouldBe HTTP_STATUS_OK

                    with(responseBody) {
                        shouldContainJsonKeyValue("uiterlijkeEinddatumAfdoening", fataleDatum.toString())
                    }
                }
            }
        }
    }
})
