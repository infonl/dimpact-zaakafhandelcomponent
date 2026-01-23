/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_ADVIES_CODE
import nl.info.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_ADVIES_NAME
import nl.info.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_AFZENDER_CODE
import nl.info.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_AFZENDER_NAME
import nl.info.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_COMMUNICATIEKANAAL_CODE
import nl.info.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_COMMUNICATIEKANAAL_NAME
import nl.info.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_DOMEIN_CODE
import nl.info.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_DOMEIN_NAME
import nl.info.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_SERVER_ERROR_ERROR_PAGINA_TEKST_CODE
import nl.info.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_SERVER_ERROR_ERROR_PAGINA_TEKST_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields
import org.json.JSONArray
import org.json.JSONObject

@Suppress("MagicNumber")
class ReferenceTableRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    var communicationChannelPageReferenceTableId = 0
    var serverErrorTextErrorPageReferenceTableId = 0

    Given("Default reference table data is provisioned on startup") {
        When("the reference tables are listed") {
            val response = itestHttpClient.performGetRequest(
                "$ZAC_API_URI/referentietabellen"
            )
            Then(
                """the provisioned default reference tables are returned"""
            ) {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                with(responseBody) {
                    shouldEqualJsonIgnoringExtraneousFields(
                        """
                        [
                            {
                                "code": "$REFERENCE_TABLE_ADVIES_CODE", 
                                "naam": "$REFERENCE_TABLE_ADVIES_NAME", 
                                "systeem": true, 
                                "aantalWaarden": 5
                            },
                            {
                                "code": "$REFERENCE_TABLE_AFZENDER_CODE", 
                                "naam": "$REFERENCE_TABLE_AFZENDER_NAME", 
                                "systeem": true, 
                                "aantalWaarden": 0
                            },
                            {
                                "code": "$REFERENCE_TABLE_COMMUNICATIEKANAAL_CODE", 
                                "naam": "$REFERENCE_TABLE_COMMUNICATIEKANAAL_NAME", 
                                "systeem": true, 
                                "aantalWaarden": 8
                            },
                            {
                                "code": "$REFERENCE_TABLE_DOMEIN_CODE", 
                                "naam": "$REFERENCE_TABLE_DOMEIN_NAME", 
                                "systeem": true, 
                                "aantalWaarden": 1
                            },
                            {
                                "code": "$REFERENCE_TABLE_SERVER_ERROR_ERROR_PAGINA_TEKST_CODE", 
                                "naam": "$REFERENCE_TABLE_SERVER_ERROR_ERROR_PAGINA_TEKST_NAME", 
                                "systeem": true, 
                                "aantalWaarden": 0
                            }
                        ]
                        """.trimIndent()
                    )
                }
                with(JSONArray(responseBody)) {
                    communicationChannelPageReferenceTableId = getJSONObject(2).getInt("id")
                    serverErrorTextErrorPageReferenceTableId = getJSONObject(4).getInt("id")
                }
            }
        }
        When("the get afzenders endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                "$ZAC_API_URI/referentietabellen/afzender"
            )
            Then(
                """an empty list should be returned since we do not provision any default afzenders"""
            ) {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                JSONArray(responseBody).length() shouldBe 0
            }
        }
        When("the communication channels reference table is retrieved") {
            val response = itestHttpClient.performGetRequest(
                "$ZAC_API_URI/referentietabellen/$communicationChannelPageReferenceTableId"
            )
            Then(
                """the provisioned default communicatiekanalen are returned including 'E-formulier'"""
            ) {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                with(JSONObject(responseBody).toString()) {
                    shouldEqualJsonIgnoringExtraneousFields(
                        """
                        {
                            "code": "$REFERENCE_TABLE_COMMUNICATIEKANAAL_CODE",
                            "naam": "$REFERENCE_TABLE_COMMUNICATIEKANAAL_NAME",
                            "systeem": true,
                            "aantalWaarden": 8,
                            "waarden": [
                                {"naam": "Balie", "systemValue": false},
                                {"naam": "E-formulier", "systemValue": true},
                                {"naam": "E-mail", "systemValue": false},
                                {"naam": "Intern", "systemValue": false},
                                {"naam": "Internet", "systemValue": false},
                                {"naam": "Medewerkersportaal", "systemValue": false},
                                {"naam": "Post", "systemValue": false},
                                {"naam": "Telefoon", "systemValue": false}
                            ]
                        }
                        """.trimIndent()
                    )
                    shouldContainJsonKey("id")
                }
            }
        }
        When("the get communication channels endpoint is called with 'true' as parameter") {
            val response = itestHttpClient.performGetRequest(
                "$ZAC_API_URI/referentietabellen/communicatiekanaal/true"
            )
            Then(
                """the provisioned default communicatiekanalen are returned including 'E-formulier'"""
            ) {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                with(JSONArray(responseBody)) {
                    length() shouldBe 8
                    shouldContainInOrder(
                        listOf(
                            "Balie",
                            "E-formulier",
                            "E-mail",
                            "Intern",
                            "Internet",
                            "Medewerkersportaal",
                            "Post",
                            "Telefoon"
                        )
                    )
                }
            }
        }
        When("the get domeinen endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                "$ZAC_API_URI/referentietabellen/domein"
            )
            Then(
                """the provisioned default domeinen are returned"""
            ) {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                with(JSONArray(responseBody)) {
                    length() shouldBe 1
                    get(0) shouldBe "domein_overig"
                }
            }
        }
        When("the get server error texts endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                "$ZAC_API_URI/referentietabellen/server-error-text"
            )
            Then(
                """an empty list should be returned since we do not provision any default server error texts"""
            ) {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                JSONArray(responseBody).length() shouldBe 0
            }
        }
        When("a reference value is added to the server error texts reference table and the name is updated") {
            val response = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/referentietabellen/$serverErrorTextErrorPageReferenceTableId",
                requestBodyAsString = """
                    {       
                    "naam": "Updated server error error pagina tekst",
                    "waarden":[{"naam":"dummyServerErrorErrorPageText"}]
                    }
                """.trimIndent()
            )
            Then("the response should be 'ok'") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                with(JSONObject(responseBody).toString()) {
                    shouldEqualJsonIgnoringExtraneousFields(
                        """
                        {
                            "code": "$REFERENCE_TABLE_SERVER_ERROR_ERROR_PAGINA_TEKST_CODE",
                            "naam": "Updated server error error pagina tekst",
                            "systeem": true,
                            "aantalWaarden": 1,
                            "waarden": [{"naam": "dummyServerErrorErrorPageText", "systemValue": false}]
                        }
                        """.trimIndent()
                    )
                    shouldContainJsonKey("id")
                }
            }
        }
        When("the get server error texts endpoint is called again") {
            val response = itestHttpClient.performGetRequest(
                "$ZAC_API_URI/referentietabellen/server-error-text"
            )
            Then(
                """the provisioned default server error texts are returned including the added 'test'"""
            ) {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                with(JSONArray(responseBody)) {
                    length() shouldBe 1
                    shouldContainInOrder(listOf("dummyServerErrorErrorPageText"))
                }
            }
        }
        When("a new reference table is added") {
            val referenceTableCode = "dummyReferenceTableCode1"
            val referenceTableName = "dummyReferenceTableName1"
            val response = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/referentietabellen",
                requestBodyAsString = """
                    {       
                    "code": "$referenceTableCode",
                    "naam": "$referenceTableName",
                    "waarden":[{"naam":"dummyReferenceTableValue1"}, {"naam":"dummyReferenceTableValue2"}]
                    }
                """.trimIndent()
            )
            Then("the response should be 'ok' and should return the created reference table with code in uppercase") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                with(JSONObject(responseBody).toString()) {
                    shouldEqualJsonIgnoringExtraneousFields(
                        """
                        {
                            "code": "${referenceTableCode.uppercase()}",
                            "naam": "$referenceTableName",
                            "systeem": false,
                            "aantalWaarden": 2,
                            "waarden": [
                                {"naam": "dummyReferenceTableValue1", "systemValue": false},
                                {"naam": "dummyReferenceTableValue2", "systemValue": false}
                            ]
                        }
                        """.trimIndent()
                    )
                    shouldContainJsonKey("id")
                }
            }
        }
    }
})
