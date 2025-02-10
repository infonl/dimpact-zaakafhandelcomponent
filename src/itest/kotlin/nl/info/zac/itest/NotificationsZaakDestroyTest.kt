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
import nl.info.zac.itest.config.ItestConfiguration.OPEN_NOTIFICATIONS_API_SECRET_KEY
import nl.info.zac.itest.config.ItestConfiguration.OPEN_ZAAK_BASE_URI
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_A_DESCRIPTION
import nl.info.zac.itest.config.ItestConfiguration.TEST_GROUP_A_ID
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_SEARCH
import nl.info.zac.itest.config.ItestConfiguration.ZAAKTYPE_INDIENEN_AANSPRAKELIJKSTELLING_DOOR_DERDEN_BEHANDELEN_UUID
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import okhttp3.Headers
import org.json.JSONObject
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

/**
 * This test creates a zaak and because we do not want this test to impact [ZoekenRESTServiceTest]
 * we run it afterward.
 */
@Order(TEST_SPEC_ORDER_AFTER_SEARCH)
class NotificationsZaakDestroyTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()

    Given("Existing process data and Solr index data for a specific zaak in ZAC") {
        lateinit var zaakUUID: UUID
        lateinit var zaakIdentificatie: String
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
            with(JSONObject(searchResponseBody).getJSONArray("resultaten").getJSONObject(0).toString()) {
                shouldContainJsonKeyValue("identificatie", zaakIdentificatie)
            }
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
                """the response should be 'no content', and the zaak should be removed from the Solr index"""
            ) {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_STATUS_NO_CONTENT
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
