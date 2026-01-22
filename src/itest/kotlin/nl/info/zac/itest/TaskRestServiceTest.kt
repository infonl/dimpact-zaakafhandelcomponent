/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldBeJsonArray
import io.kotest.assertions.json.shouldBeJsonObject
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.TaskHelper
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.BEHANDELAARS_DOMAIN_TEST_1
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.BEHANDELAAR_DOMAIN_TEST_1
import nl.info.zac.itest.config.COORDINATOR_DOMAIN_TEST_1
import nl.info.zac.itest.config.ItestConfiguration
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2000_01_01
import nl.info.zac.itest.config.ItestConfiguration.FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE
import nl.info.zac.itest.config.ItestConfiguration.HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM
import nl.info.zac.itest.config.ItestConfiguration.SCREEN_EVENT_TYPE_TAKEN_VERDELEN
import nl.info.zac.itest.config.ItestConfiguration.SCREEN_EVENT_TYPE_TAKEN_VRIJGEVEN
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.RAADPLEGER_DOMAIN_TEST_1
import nl.info.zac.itest.util.WebSocketTestListener
import nl.info.zac.itest.util.shouldEqualJsonIgnoringOrderAndExtraneousFields
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.time.LocalDate
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class TaskRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient(itestHttpClient)
    val taskHelper = TaskHelper(zacClient)
    lateinit var taskArray: JSONArray
    lateinit var zaakUuid: String
    lateinit var zaakIdentification: String
    lateinit var task1ID: String

    Given(
        """
            A zaak has been created,
            two tasks of type 'aanvullende informatie' have been started for this zaak,
            and a raadpleger authorised for this zaaktype is logged in 
            """
    ) {
        zacClient.createZaak(
            zaakTypeUUID = ZAAKTYPE_TEST_2_UUID,
            groupId = BEHANDELAARS_DOMAIN_TEST_1.name,
            groupName = BEHANDELAARS_DOMAIN_TEST_1.description,
            startDate = DATE_TIME_2000_01_01,
            testUser = BEHANDELAAR_DOMAIN_TEST_1
        ).run {
            logger.info { "Response: $bodyAsString" }
            code shouldBe HTTP_OK
            JSONObject(bodyAsString).run {
                zaakUuid = getString("uuid")
                zaakIdentification = getString("identificatie")
            }
        }
        task1ID = taskHelper.startAanvullendeInformatieTaskForZaak(
            zaakUuid = zaakUuid.let(UUID::fromString),
            zaakIdentificatie = zaakIdentification,
            fatalDate = LocalDate.now().plusWeeks(1),
            group = BEHANDELAARS_DOMAIN_TEST_1,
            testUser = BEHANDELAAR_DOMAIN_TEST_1
        )
        taskHelper.startAanvullendeInformatieTaskForZaak(
            zaakUuid = zaakUuid.let(UUID::fromString),
            zaakIdentificatie = zaakIdentification,
            fatalDate = LocalDate.now().plusWeeks(1),
            group = BEHANDELAARS_DOMAIN_TEST_1,
            testUser = BEHANDELAAR_DOMAIN_TEST_1
        )

        When("the get tasks for a zaak endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/taken/zaak/$zaakUuid",
                testUser = RAADPLEGER_DOMAIN_TEST_1
            )
            Then(
                """
                    the list of taken for this zaak is returned and contains the expected task data
                    with permissions only to read the task
                    """
            ) {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                responseBody.shouldBeJsonArray()
                // two 'aanvullende informatie' tasks have been started for this zaak,
                // so there should be two (identical) tasks in the list
                JSONArray(responseBody).length() shouldBe 2
                for (task in JSONArray(responseBody)) {
                    with(task.toString()) {
                        this shouldEqualJsonIgnoringOrderAndExtraneousFields
                            """
                            {
                              "formulierDefinitieId" : "$FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE",
                              "groep" : {
                                "id" : "${BEHANDELAARS_DOMAIN_TEST_1.name}",
                                "naam" : "${BEHANDELAARS_DOMAIN_TEST_1.description}"
                              },
                              "naam" : "$HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM",
                              "rechten" : {
                                "lezen" : true,
                                "toekennen" : false,
                                "toevoegenDocument" : false,
                                "wijzigen" : false
                              },
                              "status" : "NIET_TOEGEKEND",
                              "taakdata" : { },
                              "taakdocumenten" : [ ],
                              "taakinformatie" : { },
                              "tabellen" : { },
                              "zaakIdentificatie" : "$zaakIdentification",
                              "zaakUuid" : "$zaakUuid",
                              "zaaktypeOmschrijving" : "$ZAAKTYPE_TEST_2_DESCRIPTION",
                              "zaaktypeUUID" : "$ZAAKTYPE_TEST_2_UUID"
                            }
                            """.trimIndent()
                        shouldContainJsonKey("creatiedatumTijd")
                        shouldContainJsonKey("id")
                        shouldContainJsonKey("fataledatum")
                    }
                    taskArray = JSONArray(responseBody)
                    task1ID = taskArray.getJSONObject(0).getString("id")
                }
            }
        }
    }

    Given(
        """
            A zaak has been created and a task of type 'aanvullende informatie' has been started for this zaak
            and a behandelaar authorised for this zaaktype is logged in 
            """
    ) {
        When("the update task endpoint is called") {
            val taskObject = taskArray.getJSONObject(0)
            taskObject.put("toelichting", "update")

            val response = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/taken/taakdata",
                requestBodyAsString = taskArray.toString(),
                testUser = BEHANDELAAR_DOMAIN_TEST_1
            )

            Then("the taak has been updated successfully") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                responseBody.shouldBeJsonObject()
                responseBody.shouldContainJsonKeyValue("toelichting", "update")
            }
        }
    }

    Given(
        """
            A task has been started and a websocket subscription has been created to listen for a 'taken verdelen'
            screen event which will be sent by the asynchronous 'assign taken from list' job
            and a coordinator authorised for this zaaktype is logged in
        """.trimMargin()
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
            webSocketListener = websocketListener,
            testUser = COORDINATOR_DOMAIN_TEST_1
        )
        When("the assign tasks endpoint is called for this task") {
            val assignTasksResponse = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/taken/lijst/verdelen",
                requestBodyAsString = """{
                        "taken": [ { "taakId": "$task1ID", "zaakUuid": "$zaakUuid" } ],
                        "groepId": "${BEHANDELAARS_DOMAIN_TEST_1.name}",
                        "behandelaarGebruikersnaam": "${BEHANDELAAR_1.username}",
                        "reden": "fakeTasksAssignReason",
                        "screenEventResourceId": "$uniqueResourceId"
                        }
                """.trimIndent(),
                testUser = COORDINATOR_DOMAIN_TEST_1
            )
            Then("the task is assigned correctly") {
                val assignTasksResponseBody = assignTasksResponse.bodyAsString
                logger.info { "Response: $assignTasksResponseBody" }
                assignTasksResponse.code shouldBe HTTP_NO_CONTENT
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
        """
            |A task has been started and a websocket subscription has been created to listen for a 'taken vrijgeven'
            |screen event which will be sent by the asynchronous 'release taken from list' job
            |and a coordinator authorised for this zaaktype is logged in
        """.trimMargin()
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
                requestBodyAsString = """
                    {
                        "taken": [ { "taakId": "$task1ID", "zaakUuid": "$zaakUuid" } ],
                        "reden": "fakeTasksReleaseReason",
                        "screenEventResourceId": "$uniqueResourceId"
                    }
                """.trimIndent(),
                testUser = COORDINATOR_DOMAIN_TEST_1
            )
            Then("the task is released correctly") {
                val assignTasksResponseBody = releaseTasksResponse.bodyAsString
                logger.info { "Response: $assignTasksResponseBody" }
                releaseTasksResponse.code shouldBe HTTP_NO_CONTENT
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
