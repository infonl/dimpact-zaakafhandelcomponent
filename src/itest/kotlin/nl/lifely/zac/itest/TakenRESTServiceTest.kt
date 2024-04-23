/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldBeJsonArray
import io.kotest.assertions.json.shouldBeJsonObject
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.json.shouldNotContainJsonKey
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.config.ItestConfiguration
import nl.lifely.zac.itest.config.ItestConfiguration.FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE
import nl.lifely.zac.itest.config.ItestConfiguration.HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM
import nl.lifely.zac.itest.config.ItestConfiguration.SCREEN_EVENT_TYPE_TAKEN_VERDELEN
import nl.lifely.zac.itest.config.ItestConfiguration.SCREEN_EVENT_TYPE_TAKEN_VRIJGEVEN
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_GROUP_A_DESCRIPTION
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_GROUP_A_ID
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_TASK_CREATED
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_USER_2_ID
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_1_IDENTIFICATION
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.lifely.zac.itest.config.ItestConfiguration.task1ID
import nl.lifely.zac.itest.config.ItestConfiguration.zaak1UUID
import nl.lifely.zac.itest.util.WebSocketTestListener
import org.json.JSONArray
import org.json.JSONObject
import org.mockserver.model.HttpStatusCode
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

/**
 * This test assumes a human task plan item (=task) has been started for a zaak in a previously run test.
 */
@Order(TEST_SPEC_ORDER_AFTER_TASK_CREATED)
class TakenRESTServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    Given("A zaak has been created and a task of type 'aanvullende informatie' has been started for this zaak") {
        lateinit var responseBody: String

        When("the get tasks for a zaak endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                "$ZAC_API_URI/taken/zaak/$zaak1UUID"
            )
            Then(
                """the list of taken for this zaak is returned and contains the expected task"""
            ) {
                responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                responseBody.shouldBeJsonArray()
                // the zaak is in the intake phase, so there should be only be one human task
                // plan item: 'aanvullende informatie'
                JSONArray(responseBody).length() shouldBe 1
                with(JSONArray(responseBody)[0].toString()) {
                    shouldContainJsonKeyValue("naam", HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM)
                    shouldContainJsonKeyValue(
                        "formulierDefinitieId",
                        FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE
                    )
                    shouldContainJsonKeyValue("status", "NIET_TOEGEKEND")
                    shouldContainJsonKeyValue("zaakIdentificatie", ZAAK_1_IDENTIFICATION)
                    shouldContainJsonKeyValue(
                        "zaaktypeOmschrijving",
                        ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION
                    )
                    shouldContainJsonKeyValue("zaakUuid", zaak1UUID.toString())
                    JSONObject(this,).getJSONObject("groep").apply {
                        getString("id") shouldBe TEST_GROUP_A_ID
                        getString("naam") shouldBe TEST_GROUP_A_DESCRIPTION
                    }
                    shouldContainJsonKey("id")
                    shouldNotContainJsonKey("toelichting")
                    task1ID = JSONObject(this).getString("id")
                }
            }
        }
        When("the update task endpoint is called") {
            val taskArray = JSONArray(responseBody)
            val taskObject = taskArray.getJSONObject(0)
            taskObject.put("toelichting", "update")

            val response = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/taken/taakdata",
                requestBodyAsString = taskArray.toString()
            )

            Then("the taak has been updated successfully") {
                responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                responseBody.shouldBeJsonObject()
                responseBody.shouldContainJsonKeyValue("toelichting", "update")
            }
        }
    }
    Given(
        """A task has been started and a websocket subscription has been created to listen
                for a 'taken verdelen' screen event which will be sent by the asynchronous 'assign taken from list' job"""
    ) {
        val uniqueResourceId = UUID.randomUUID()
        val websocketListener = WebSocketTestListener(
            textToBeSentOnOpen = "{" +
                "\"subscriptionType\":\"CREATE\"," +
                "\"event\":{" +
                "  \"opcode\":\"UPDATED\"," +
                "  \"objectType\":\"$SCREEN_EVENT_TYPE_TAKEN_VERDELEN\"," +
                "  \"objectId\":{" +
                "    \"resource\":\"$uniqueResourceId\"" +
                "  }," +
                "\"_key\":\"ANY;$SCREEN_EVENT_TYPE_TAKEN_VERDELEN;$uniqueResourceId\"" +
                "}" +
                "}"
        )
        itestHttpClient.connectNewWebSocket(
            url = ItestConfiguration.ZAC_WEBSOCKET_BASE_URI,
            webSocketListener = websocketListener
        )
        When("the assign tasks endpoint is called for this task") {
            val assignTasksResponse = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/taken/lijst/verdelen",
                requestBodyAsString = """{
                        "taken":[{"taakId":"$task1ID","zaakUuid":"$zaak1UUID"}],
                         "groepId":"$TEST_GROUP_A_ID",
                        "behandelaarGebruikersnaam":"$TEST_USER_2_ID",
                        "reden":"dummyTasksAssignReason",
                        "screenEventResourceId":"$uniqueResourceId"
                        }
                """.trimIndent()
            )
            Then("the task is assigned correctly") {
                val assignTasksResponseBody = assignTasksResponse.body!!.string()
                logger.info { "Response: $assignTasksResponseBody" }
                assignTasksResponse.code shouldBe HttpStatusCode.NO_CONTENT_204.code()
                // the backend process is asynchronous, so we need to wait a bit until the tasks are assigned
                eventually(10.seconds) {
                    websocketListener.messagesReceived.size shouldBe 1
                    with(JSONObject(websocketListener.messagesReceived[0])) {
                        getString("opcode") shouldBe "UPDATED"
                        getString("objectType") shouldBe SCREEN_EVENT_TYPE_TAKEN_VERDELEN
                        getJSONObject("objectId").getString("resource") shouldBe uniqueResourceId.toString()
                    }
                }
            }
        }
    }
    Given(
        """A task has been started and a websocket subscription has been created to listen
                for a 'taken vrijgeven' screen event which will be sent by the asynchronous 'release taken from list' job"""
    ) {
        val uniqueResourceId = UUID.randomUUID()
        val websocketListener = WebSocketTestListener(
            textToBeSentOnOpen = "{" +
                "\"subscriptionType\":\"CREATE\"," +
                "\"event\":{" +
                "  \"opcode\":\"UPDATED\"," +
                "  \"objectType\":\"$SCREEN_EVENT_TYPE_TAKEN_VRIJGEVEN\"," +
                "  \"objectId\":{" +
                "    \"resource\":\"$uniqueResourceId\"" +
                "  }," +
                "\"_key\":\"ANY;$SCREEN_EVENT_TYPE_TAKEN_VRIJGEVEN;$uniqueResourceId\"" +
                "}" +
                "}"
        )
        itestHttpClient.connectNewWebSocket(
            url = ItestConfiguration.ZAC_WEBSOCKET_BASE_URI,
            webSocketListener = websocketListener
        )
        When("the release tasks endpoint is called for this task") {
            val releaseTasksResponse = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/taken/lijst/vrijgeven",
                requestBodyAsString = """{
                        "taken":[{"taakId":"$task1ID","zaakUuid":"$zaak1UUID"}],
                        "reden":"dummyTasksReleaseReason",
                        "screenEventResourceId":"$uniqueResourceId"
                        }
                """.trimIndent()
            )
            Then("the task is released correctly") {
                val assignTasksResponseBody = releaseTasksResponse.body!!.string()
                logger.info { "Response: $assignTasksResponseBody" }
                releaseTasksResponse.code shouldBe HttpStatusCode.NO_CONTENT_204.code()
                // the backend process is asynchronous, so we need to wait a bit until the tasks are released
                eventually(10.seconds) {
                    websocketListener.messagesReceived.size shouldBe 1
                    with(JSONObject(websocketListener.messagesReceived[0])) {
                        getString("opcode") shouldBe "UPDATED"
                        getString("objectType") shouldBe SCREEN_EVENT_TYPE_TAKEN_VRIJGEVEN
                        getJSONObject("objectId").getString("resource") shouldBe uniqueResourceId.toString()
                    }
                }
            }
        }
    }
})
