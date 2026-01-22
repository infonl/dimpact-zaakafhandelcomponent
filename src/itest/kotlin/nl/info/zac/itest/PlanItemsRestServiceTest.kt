/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldBeJsonArray
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.authenticate
import nl.info.zac.itest.config.BEHANDELAARS_DOMAIN_TEST_1
import nl.info.zac.itest.config.BEHANDELAAR_DOMAIN_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE
import nl.info.zac.itest.config.ItestConfiguration.HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM
import nl.info.zac.itest.config.ItestConfiguration.HUMAN_TASK_TYPE
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_3_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

const val UITERLIJKE_EINDDATUM_AFDOENING = "2000-01-15"

class PlanItemsRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()
    lateinit var humanTaskItemAanvullendeInformatieId: String

    Given("A zaak has been created for test zaaktype 3 and a behandelaar is logged in") {
        lateinit var zaakUuid: UUID
        zacClient.createZaak(
            zaakTypeUUID = ZAAKTYPE_TEST_3_UUID,
            groupId = BEHANDELAARS_DOMAIN_TEST_1.name,
            groupName = BEHANDELAARS_DOMAIN_TEST_1.description,
            startDate = DATE_TIME_2000_01_01,
            testUser = BEHANDELAAR_DOMAIN_TEST_1
        ).run {
            code shouldBe HTTP_OK
            JSONObject(bodyAsString).run {
                zaakUuid = getString("uuid").run(UUID::fromString)
            }
        }

        When("the list human task plan items endpoint is called") {
            val response = zacClient.getHumanTaskPlanItemsForZaak(zaakUuid, BEHANDELAAR_DOMAIN_TEST_1)

            Then("the list of human task plan items for this zaak contains the task 'aanvullende informatie'") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                responseBody.shouldBeJsonArray()
                // the zaak is in the intake phase, so there should be only be one human task
                // plan item: 'aanvullende informatie'
                JSONArray(responseBody).length() shouldBe 1
                with(JSONArray(responseBody)[0].toString()) {
                    shouldContainJsonKeyValue("actief", "true")
                    shouldContainJsonKeyValue("formulierDefinitie", FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE)
                    shouldContainJsonKeyValue("naam", HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM)
                    shouldContainJsonKeyValue("type", HUMAN_TASK_TYPE)
                    shouldContainJsonKeyValue("zaakUuid", zaakUuid.toString())
                    shouldContainJsonKey("id")
                }
                humanTaskItemAanvullendeInformatieId = JSONArray(responseBody).getJSONObject(0).getString("id")
            }
        }

        When("the get human task plan item endpoint is called for the task 'aanvullende informatie'") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/planitems/humanTaskPlanItem/$humanTaskItemAanvullendeInformatieId",
                testUser = BEHANDELAAR_DOMAIN_TEST_1
            )
            Then("the human task plan item data for this task is returned") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                with(responseBody) {
                    shouldContainJsonKeyValue("actief", "true")
                    shouldContainJsonKeyValue("formulierDefinitie", FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE)
                    shouldContainJsonKeyValue("naam", HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM)
                    shouldContainJsonKeyValue("type", HUMAN_TASK_TYPE)
                    shouldContainJsonKeyValue("zaakUuid", zaakUuid.toString())
                    shouldContainJsonKeyValue("id", humanTaskItemAanvullendeInformatieId)
                }
            }
        }

        When("the start human task plan items endpoint is called with a fatal date") {
            val response = zacClient.startHumanTaskPlanItem(
                planItemInstanceId = humanTaskItemAanvullendeInformatieId,
                fatalDate = LocalDate.parse(UITERLIJKE_EINDDATUM_AFDOENING).minusDays(1),
                groupId = BEHANDELAARS_DOMAIN_TEST_1.name,
                groupName = BEHANDELAARS_DOMAIN_TEST_1.description,
                testUser = BEHANDELAAR_DOMAIN_TEST_1
            )

            Then("a task is started for this zaak") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_NO_CONTENT
            }
        }

        When("creation of a new additional info task with fatal date past the zaak fatal date is requested") {
            val newAdditionalTaskInfoResponse = itestHttpClient.performGetRequest(
                "$ZAC_API_URI/planitems/zaak/$zaakUuid/humanTaskPlanItems",
                testUser = BEHANDELAAR_DOMAIN_TEST_1
            )
            val newAdditionalTaskInfoResponseBody = newAdditionalTaskInfoResponse.bodyAsString
            logger.info { "Response: $newAdditionalTaskInfoResponseBody" }
            newAdditionalTaskInfoResponse.code shouldBe HTTP_OK
            val newAdditionalInfoTaskId = JSONArray(newAdditionalTaskInfoResponseBody).getJSONObject(0).getString("id")

            val fataleDatum = LocalDate.parse(UITERLIJKE_EINDDATUM_AFDOENING)
                .plusDays(2)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val response = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/planitems/doHumanTaskPlanItem",
                requestBodyAsString = """{
                        "planItemInstanceId": "$newAdditionalInfoTaskId",
                        "fataledatum": "$fataleDatum",
                        "taakStuurGegevens": { "sendMail": false },
                        "groep": { "id": "${BEHANDELAARS_DOMAIN_TEST_1.name}", "naam": "${BEHANDELAARS_DOMAIN_TEST_1.description}" },
                        "taakdata":{}
                    }
                    """.trimIndent(),
                testUser = BEHANDELAAR_DOMAIN_TEST_1
            )

            Then("a new task is started for this zaak") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_NO_CONTENT
            }

            And("zaak fatal date is moved forward to correspond to the task fatal date") {
                val zacResponse = zacClient.retrieveZaak(zaakUuid, BEHANDELAAR_DOMAIN_TEST_1)
                val responseBody = zacResponse.bodyAsString
                logger.info { "Response: $responseBody" }
                with(zacResponse) {
                    code shouldBe HTTP_OK
                    with(responseBody) {
                        shouldContainJsonKeyValue("uiterlijkeEinddatumAfdoening", fataleDatum.toString())
                    }
                }
            }
        }
    }
})
