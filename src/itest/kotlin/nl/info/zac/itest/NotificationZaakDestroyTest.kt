/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.annotation.Isolate
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.config.ItestConfiguration.ACTIE_INTAKE_AFRONDEN
import nl.info.zac.itest.config.ItestConfiguration.ACTIE_ZAAK_AFHANDELEN
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_BPMN_TEST_1_UUID
import nl.info.zac.itest.config.RECORDMANAGER_1
import nl.info.zac.itest.config.TestUser
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.client.createZaakAndRetrieve
import nl.info.zac.itest.config.BEHANDELAAR_1
import nl.info.zac.itest.config.GROUP_BEHANDELAARS_TEST_1
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2024_01_31
import nl.info.zac.itest.config.ItestConfiguration.OPEN_NOTIFICATIONS_API_SECRET_KEY
import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_CMMN_TEST_2_UUID
import nl.info.zac.itest.config.ItestConfiguration.TEST_AANVULLENDE_INFORMATIE_EMAIL
import nl.info.zac.itest.config.ItestConfiguration.TEST_AANVULLENDE_INFORMATIE_MAIL_BODY
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestConfiguration.ZAC_INTERNAL_ENDPOINTS_API_KEY
import nl.info.zac.itest.util.sleepForOpenZaakUniqueConstraint
import nl.info.zac.itest.util.waitForReindexToFinish
import nl.info.zac.itest.util.zacContainerLogs
import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_ACCEPTED
import java.net.HttpURLConnection.HTTP_NOT_FOUND
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

/**
 * These tests cover a very hypothetical case: they send a 'zaak destroy' notification for a zaak that still
 * exists in the zaakregister. In normal operation, a 'zaak destroy' notification is only ever sent after the zaak
 * has been deleted from the zaakregister. If such a notification is sent for an existing zaak, something is very
 * wrong in the platform.
 */
@Isolate
class NotificationZaakDestroyTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()

    fun createZaak(zaaktypeUuid: UUID, testUser: TestUser): UUID =
        zacClient.createZaakAndRetrieve(
            zaakTypeUUID = zaaktypeUuid,
            groupId = GROUP_BEHANDELAARS_TEST_1.name,
            groupName = GROUP_BEHANDELAARS_TEST_1.description,
            startDate = DATE_TIME_2024_01_31,
            testUser = testUser
        ).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            code shouldBe HTTP_OK
            JSONObject(responseBody).getString("uuid").run(UUID::fromString)
        }

    fun readZaak(zaakUuid: UUID, testUser: TestUser): String =
        zacClient.retrieveZaak(zaakUuid, testUser).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            code shouldBe HTTP_OK
            responseBody
        }

    fun countTaken(zaakUuid: UUID, testUser: TestUser): Int =
        itestHttpClient.performGetRequest(url = "$ZAC_API_URI/taken/zaak/$zaakUuid", testUser = testUser).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            code shouldBe HTTP_OK
            JSONArray(responseBody).length()
        }

    fun doUserEventListenerPlanItem(zaakUuid: UUID, actionJson: String) {
        val planItemInstanceId = itestHttpClient.performGetRequest(
            url = "$ZAC_API_URI/planitems/zaak/$zaakUuid/userEventListenerPlanItems",
            testUser = RECORDMANAGER_1
        ).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            JSONArray(responseBody).getJSONObject(0).getString("id")
        }
        sleepForOpenZaakUniqueConstraint(1)
        itestHttpClient.performJSONPostRequest(
            "$ZAC_API_URI/planitems/doUserEventListenerPlanItem",
            requestBodyAsString = """
                {
                    "zaakUuid": "$zaakUuid",
                    "planItemInstanceId": "$planItemInstanceId",
                    $actionJson
                }
            """.trimIndent(),
            testUser = RECORDMANAGER_1
        ).run {
            code shouldBe HTTP_NO_CONTENT
        }
    }

    fun sendZaakDestroyNotification(zaakUuid: UUID) =
        itestHttpClient.performJSONPostRequest(
            url = "$ZAC_API_URI/notificaties",
            headers = Headers.headersOf(
                "Content-Type",
                "application/json",
                "Authorization",
                OPEN_NOTIFICATIONS_API_SECRET_KEY
            ),
            requestBodyAsString = JSONObject(
                mapOf(
                    "kanaal" to "zaken",
                    "resource" to "zaak",
                    "hoofdObject" to "$OPEN_ZAAK_BASE_URI/zaken/api/v1/zaken/$zaakUuid",
                    "resourceUrl" to "$OPEN_ZAAK_BASE_URI/zaken/api/v1/zaken/$zaakUuid",
                    "actie" to "destroy",
                    "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString()
                )
            ).toString()
        )

    given(
        """
            A zaak in ZAC which has been started using the ZAC CMMN model 
            and which has been indexed in the Solr search index,
            and a logged-in behandelaar
        """.trimIndent()
    ) {
        lateinit var zaakUUID: UUID
        lateinit var zaakIdentificatie: String
        lateinit var humanTaskItemAanvullendeInformatieId: String
        lateinit var aanvullendeInformatieTaskID: String
        zacClient.createZaakAndRetrieve(
            zaakTypeUUID = ZAAKTYPE_CMMN_TEST_2_UUID,
            groupId = GROUP_BEHANDELAARS_TEST_1.name,
            groupName = GROUP_BEHANDELAARS_TEST_1.description,
            behandelaarId = BEHANDELAAR_1.username,
            startDate = DATE_TIME_2024_01_31,
            testUser = BEHANDELAAR_1
        ).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            this.code shouldBe HTTP_OK
            JSONObject(responseBody).run {
                zaakUUID = getString("uuid").run(UUID::fromString)
                zaakIdentificatie = getString("identificatie")
            }
        }
        // retrieve the human task plan items for the zaak so that we can start the task 'aanvullende informatie'
        itestHttpClient.performGetRequest(
            url = "$ZAC_API_URI/planitems/zaak/$zaakUUID/humanTaskPlanItems",
            testUser = BEHANDELAAR_1
        ).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            this.code shouldBe HTTP_OK
            humanTaskItemAanvullendeInformatieId = JSONArray(responseBody).getJSONObject(0).getString("id")
        }
        sleepForOpenZaakUniqueConstraint(1)
        // start the human task plan item (=task) 'aanvullende informatie'
        itestHttpClient.performJSONPostRequest(
            url = "$ZAC_API_URI/planitems/doHumanTaskPlanItem",
            requestBodyAsString = """
                {
                    "planItemInstanceId": "$humanTaskItemAanvullendeInformatieId",
                    "groep": {"id":"${GROUP_BEHANDELAARS_TEST_1.name}", "naam":"${GROUP_BEHANDELAARS_TEST_1.description}"},
                    "taakdata": {
                        "emailadres": "$TEST_AANVULLENDE_INFORMATIE_EMAIL",
                        "body": "$TEST_AANVULLENDE_INFORMATIE_MAIL_BODY"
                    }
                }
            """.trimIndent(),
            testUser = BEHANDELAAR_1
        ).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            this.code shouldBe HTTP_NO_CONTENT
        }
        // get the list of taken for the zaak to set the task ID for the 'aanvullende informatie' task
        itestHttpClient.performGetRequest(
            url = "$ZAC_API_URI/taken/zaak/$zaakUUID",
            testUser = BEHANDELAAR_1
        ).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            this.code shouldBe HTTP_OK
            JSONArray(responseBody).length() shouldBe 1
            aanvullendeInformatieTaskID = JSONArray(responseBody).getJSONObject(0).getString("id")
        }
        // reindex so that the new zaak gets added to the Solr index
        val logsBeforeReindex = zacContainerLogs()
        itestHttpClient.performGetRequest(
            url = "$ZAC_API_URI/internal/indexeren/herindexeren/ZAAK",
            headers = mapOf(
                "Content-Type" to "application/json",
                "X-API-KEY" to ZAC_INTERNAL_ENDPOINTS_API_KEY
            ).toHeaders(),
            testUser = BEHANDELAAR_1
        ).run {
            logger.info { "Response: $bodyAsString" }
            this.code shouldBe HTTP_ACCEPTED
        }
        // reindexing now runs asynchronously in the background, so wait for it to fully finish before
        // proceeding: otherwise a still-running reindex could re-add the zaak this test later removes
        // from the Solr index, since the zaak is deliberately never actually deleted from OpenZaak
        waitForReindexToFinish("ZAAK", logsBeforeReindex)
        // wait for the indexing to complete
        eventually(10.seconds) {
            val searchResponseBody = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/zoeken/list",
                requestBodyAsString = """
                    {
                        "alleenMijnZaken": false,
                        "alleenOpenstaandeZaken": true,
                        "alleenAfgeslotenZaken": false,
                        "alleenMijnTaken": false,
                        "zoeken": { "ALLE": "$zaakIdentificatie" }, 
                        "filters": {},                            
                        "datums": {},
                        "rows": 10,
                        "page": 0                        
                    }
                """.trimIndent(),
                testUser = BEHANDELAAR_1
            ).bodyAsString
            JSONObject(searchResponseBody).getInt("totaal") shouldBe 1
            searchResponseBody.shouldContainJsonKeyValue("$.resultaten[0].identificatie", zaakIdentificatie)
        }
        `when`("the notificaties endpoint is called with a 'zaak destroy' payload") {
            val response = sendZaakDestroyNotification(zaakUUID)
            then(
                """
                    the response should be 'no content', the Flowable CMMN zaak data should be deleted,
                    the task should be deleted and the zaak should be removed from the Solr index
                """.trimIndent()
            ) {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_NO_CONTENT
                // Retrieve the zaak and check that the zaakdata is no longer available.
                // Note that in this test scenario the zaak is not deleted from OpenZaak
                // and so ZAC should still return the zaak.
                // However, all Flowable data related to the zaak should be deleted.
                zacClient.retrieveZaak(zaakUUID, BEHANDELAAR_1).run {
                    val responseBody = this.bodyAsString
                    logger.info { "Response: $responseBody" }
                    this.code shouldBe HTTP_OK
                    responseBody.shouldContainJsonKeyValue("uuid", zaakUUID.toString())
                    responseBody.shouldContainJsonKeyValue("zaakdata", "")
                }
                // check that there are no tasks left for the zaak
                // any tasks should have been deleted as part of the 'zaak destroy' action
                itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/taken/zaak/$zaakUUID",
                    testUser = BEHANDELAAR_1
                ).run {
                    val responseBody = bodyAsString
                    logger.info { "Response: $responseBody" }
                    this.code shouldBe HTTP_OK
                    JSONArray(responseBody).length() shouldBe 0
                }
                // to be sure, also explicitly check if the task that was started earlier has been deleted
                itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/taken/$aanvullendeInformatieTaskID",
                    testUser = BEHANDELAAR_1
                ).run {
                    val responseBody = bodyAsString
                    logger.info { "Response: $responseBody" }
                    this.code shouldBe HTTP_NOT_FOUND
                    responseBody shouldEqualJson """
                        {"message":"No historic task with id '$aanvullendeInformatieTaskID' found"}
                    """.trimIndent()
                }
                // wait for the zaak to be removed from the Solr index
                eventually(10.seconds) {
                    val searchResponseBody = itestHttpClient.performPutRequest(
                        url = "$ZAC_API_URI/zoeken/list",
                        requestBodyAsString = """
                            {
                                "alleenMijnZaken": false,
                                "alleenOpenstaandeZaken": true,
                                "alleenAfgeslotenZaken": false,
                                "alleenMijnTaken": false,
                                "zoeken": { "ALLE": "$zaakIdentificatie" }, 
                                "filters": {},                            
                                "datums": {},
                                "rows": 10,
                                "page": 0                        
                            }
                        """.trimIndent(),
                        testUser = BEHANDELAAR_1
                    ).bodyAsString
                    JSONObject(searchResponseBody).getInt("totaal") shouldBe 0
                }
            }
        }
    }

    given("an open zaak with a running BPMN process") {
        val zaakUuid = createZaak(ZAAKTYPE_BPMN_TEST_1_UUID, BEHANDELAAR_1)
        JSONObject(readZaak(zaakUuid, BEHANDELAAR_1)).getJSONObject("zaakdata").getString("zaakUUID") shouldBe
            zaakUuid.toString()

        `when`("the zaak destroy notification is received") {
            val response = sendZaakDestroyNotification(zaakUuid)

            then("the zaak has no zaakdata and no taken left, because its BPMN process and history are deleted") {
                response.code shouldBe HTTP_NO_CONTENT
                readZaak(zaakUuid, BEHANDELAAR_1).shouldContainJsonKeyValue("zaakdata", "")
                countTaken(zaakUuid, BEHANDELAAR_1) shouldBe 0
            }
        }
    }

    given("a terminated zaak whose BPMN process has ended, so that its zaakdata comes from the process history") {
        val zaakUuid = createZaak(ZAAKTYPE_BPMN_TEST_1_UUID, BEHANDELAAR_1)
        itestHttpClient.performPatchRequest(
            url = "$ZAC_API_URI/zaken/zaak/$zaakUuid/afbreken",
            requestBodyAsString = """{ "zaakbeeindigRedenId": "ZAAK_NIET_ONTVANKELIJK" }""",
            testUser = BEHANDELAAR_1
        ).run {
            logger.info { "Response: $bodyAsString" }
            code shouldBe HTTP_OK
        }
        JSONObject(readZaak(zaakUuid, BEHANDELAAR_1)).getJSONObject("zaakdata").getString("zaakUUID") shouldBe
            zaakUuid.toString()

        `when`("the zaak destroy notification is received") {
            val response = sendZaakDestroyNotification(zaakUuid)

            then("the zaak has no zaakdata and no taken left, because the history of its BPMN process is deleted") {
                response.code shouldBe HTTP_NO_CONTENT
                readZaak(zaakUuid, BEHANDELAAR_1).shouldContainJsonKeyValue("zaakdata", "")
                countTaken(zaakUuid, BEHANDELAAR_1) shouldBe 0
            }
        }
    }

    given("a completed zaak whose CMMN case has ended, so that its zaakdata comes from the case history") {
        val zaakUuid = createZaak(ZAAKTYPE_CMMN_TEST_2_UUID, RECORDMANAGER_1)
        doUserEventListenerPlanItem(
            zaakUuid,
            """
                "actie": "$ACTIE_INTAKE_AFRONDEN",
                "isZaakOntvankelijk": true
            """.trimIndent()
        )
        val resultaattypeUuid = itestHttpClient.performGetRequest(
            url = "$ZAC_API_URI/zaken/resultaattypes/$ZAAKTYPE_CMMN_TEST_2_UUID",
            testUser = RECORDMANAGER_1
        ).run {
            val responseBody = bodyAsString
            logger.info { "Response: $responseBody" }
            JSONArray(responseBody).getJSONObject(0).getString("id")
        }
        doUserEventListenerPlanItem(
            zaakUuid,
            """
                "actie": "$ACTIE_ZAAK_AFHANDELEN",
                "resultaattypeUuid": "$resultaattypeUuid",
                "resultaatToelichting": "fakeResultaatToelichting"
            """.trimIndent()
        )
        JSONObject(readZaak(zaakUuid, RECORDMANAGER_1)).run {
            getBoolean("isOpen") shouldBe false
            getJSONObject("zaakdata").getString("zaakUUID") shouldBe zaakUuid.toString()
        }

        `when`("the zaak destroy notification is received") {
            val response = sendZaakDestroyNotification(zaakUuid)

            then("the zaak has no zaakdata and no taken left, because the history of its CMMN case is deleted") {
                response.code shouldBe HTTP_NO_CONTENT
                readZaak(zaakUuid, RECORDMANAGER_1).shouldContainJsonKeyValue("zaakdata", "")
                countTaken(zaakUuid, RECORDMANAGER_1) shouldBe 0
            }
        }
    }
})
