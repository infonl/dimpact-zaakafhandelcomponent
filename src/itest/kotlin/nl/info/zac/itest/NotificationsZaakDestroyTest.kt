/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.ItestConfiguration.DATE_TIME_2024_01_31
import nl.info.zac.itest.config.ItestConfiguration.HTTP_STATUS_NO_CONTENT
import nl.info.zac.itest.config.ItestConfiguration.HTTP_STATUS_OK
import nl.info.zac.itest.config.ItestConfiguration.OPEN_NOTIFICATIONS_API_SECRET_KEY
import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_A_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_A_ID
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_SEARCH
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestConfiguration.zaakProductaanvraag1Uuid
import okhttp3.Headers
import org.json.JSONArray
import org.json.JSONObject
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

/**
 * This test creates a zaak and a task and because we do not want this test to impact e.g. [ZoekenRESTServiceTest]
 * we run it afterward.
 */
@Order(TEST_SPEC_ORDER_AFTER_SEARCH)
class NotificationsZaakDestroyTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()

    Given(
        """
            A zaak in ZAC which has been started using the ZAC CMMN model 
            and which has been indexed in the Solr search index
        """.trimIndent()
    ) {
        lateinit var zaakUUID: UUID
        lateinit var zaakIdentificatie: String
        lateinit var humanTaskItemAanvullendeInformatieId: String
        zacClient.createZaak(
            zaakTypeUUID = ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID,
            groupId = TEST_GROUP_A_ID,
            groupName = TEST_GROUP_A_DESCRIPTION,
            startDate = DATE_TIME_2024_01_31
        ).run {
            val responseBody = body!!.string()
            logger.info { "Response: $responseBody" }
            this.isSuccessful shouldBe true
            JSONObject(responseBody).run {
                zaakUUID = getString("uuid").run(UUID::fromString)
                zaakIdentificatie = getString("identificatie")
            }
        }
        // retrieve the human task plan items for the zaak so that we can start the task 'aanvullende informatie'
        itestHttpClient.performGetRequest(
            "$ZAC_API_URI/planitems/zaak/$zaakUUID/humanTaskPlanItems"
        ).run {
            val responseBody = body!!.string()
            logger.info { "Response: $responseBody" }
            this.isSuccessful shouldBe true
            humanTaskItemAanvullendeInformatieId = JSONArray(responseBody).getJSONObject(0).getString("id")
        }
        // start the human task plan item (=task) 'aanvullende informatie'
        itestHttpClient.performJSONPostRequest(
            url = "$ZAC_API_URI/planitems/doHumanTaskPlanItem",
            requestBodyAsString = """
                {
                    "planItemInstanceId": "$humanTaskItemAanvullendeInformatieId",
                    "taakStuurGegevens": {"sendMail":false},
                    "medewerker":null,"groep":{"id":"$TEST_GROUP_A_ID","naam":"$TEST_GROUP_A_DESCRIPTION"},
                    "taakdata": { "dummyTestKey": "dummyTestValue" }
                }
            """.trimIndent()
        ).run {
            val responseBody = body!!.string()
            logger.info { "Response: $responseBody" }
            this.isSuccessful shouldBe true
        }
        // reindex so that the new zaak gets added to the Solr index
        itestHttpClient.performGetRequest(
            url = "$ZAC_API_URI/indexeren/herindexeren/ZAAK",
            addAuthorizationHeader = false
        ).run {
            logger.info { "Response: ${body!!.string()}" }
            this.isSuccessful shouldBe true
        }
        // wait for the indexing to complete
        eventually(10.seconds) {
            val searchResponseBody = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/zoeken/list",
                requestBodyAsString = """
                    {
                        "filtersType": "ZoekParameters",
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
                """.trimIndent()
            ).body!!.string()
            JSONObject(searchResponseBody).getInt("totaal") shouldBe 1
            searchResponseBody.shouldContainJsonKeyValue("$.resultaten[0].identificatie", zaakIdentificatie)
        }
        When("the notificaties endpoint is called with a 'zaak destroy' payload") {
            val response = itestHttpClient.performJSONPostRequest(
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
                        "hoofdObject" to "$OPEN_ZAAK_BASE_URI/zaken/api/v1/zaken/$zaakUUID",
                        "resourceUrl" to "$OPEN_ZAAK_BASE_URI/zaken/api/v1/zaken/$zaakUUID",
                        "actie" to "destroy",
                        "aanmaakdatum" to ZonedDateTime.now(ZoneId.of("UTC")).toString()
                    )
                ).toString(),
                addAuthorizationHeader = false
            )
            Then(
                """
                    the response should be 'no content', the Flowable CMMN zaak data should be deleted,
                    the task should be deleted and the zaak should be removed from the Solr index
                """.trimIndent()
            ) {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_STATUS_NO_CONTENT
                // Retrieve the zaak and check that the zaakdata is no longer available.
                // Note that in this test scenario the zaak is not deleted from OpenZaak
                // and so ZAC should still return the zaak.
                // However, all Flowable data related to the zaak should be deleted.
                zacClient.retrieveZaak(zaakUUID).run {
                    val responseBody = this.body!!.string()
                    logger.info { "Response: $responseBody" }
                    this.code shouldBe HTTP_STATUS_OK
                    responseBody.shouldContainJsonKeyValue("uuid", zaakUUID.toString())
                    responseBody.shouldContainJsonKeyValue("zaakdata", "")
                }
                // check that the task that was started for this zaak no longer exists
                // it should have been deleted as part of the 'zaak destroy' action
                itestHttpClient.performGetRequest(
                    url = "$ZAC_API_URI/taken/zaak/$zaakUUID"
                ).run {
                    val responseBody = body!!.string()
                    logger.info { "Response: $responseBody" }
                    this.isSuccessful shouldBe true
                    JSONArray(responseBody).length() shouldBe 0
                }
                // TODO: also call TaskRestService.listHistory (should be empty)
                // TODO: also call TaskRestService.readTask (should return 404)

                // wait for the zaak to be removed from the Solr index
                eventually(10.seconds) {
                    val searchResponseBody = itestHttpClient.performPutRequest(
                        url = "$ZAC_API_URI/zoeken/list",
                        requestBodyAsString = """
                    {
                        "filtersType": "ZoekParameters",
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
                        """.trimIndent()
                    ).body!!.string()
                    JSONObject(searchResponseBody).getInt("totaal") shouldBe 0
                }
            }
        }
    }
})
