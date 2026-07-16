/*
 * SPDX-FileCopyrightText: 2023 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.config.BEHEERDER_1
import nl.info.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_ADVIES_CODE
import nl.info.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_ADVIES_NAME
import nl.info.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_AFZENDER_CODE
import nl.info.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_AFZENDER_NAME
import nl.info.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_BRP_DOELBINDING_RAADPLEEG_WAARDE_CODE
import nl.info.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_BRP_DOELBINDING_RAADPLEEG_WAARDE_NAAM
import nl.info.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_BRP_DOELBINDING_ZOEK_WAARDE_CODE
import nl.info.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_BRP_DOELBINDING_ZOEK_WAARDE_NAAM
import nl.info.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_BRP_VERWERKINGSREGISTER_WAARDE_CODE
import nl.info.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_BRP_VERWERKINGSREGISTER_WAARDE_NAAM
import nl.info.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_COMMUNICATIEKANAAL_CODE
import nl.info.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_COMMUNICATIEKANAAL_NAME
import nl.info.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_SERVER_ERROR_ERROR_PAGINA_TEKST_CODE
import nl.info.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_SERVER_ERROR_ERROR_PAGINA_TEKST_NAME
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.RAADPLEGER_1
import nl.info.zac.itest.util.shouldEqualJsonIgnoringExtraneousFields
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_OK

@Suppress("MagicNumber")
class ReferenceTableRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    var communicationChannelReferenceTableId = 0
    var serverErrorTextErrorReferenceTableId = 0

    given(
        """Default reference table data is provisioned on startup
            and general test data reference table data is added in test setup,
            and a beheerder is logged in"""
    ) {
        `when`("the reference tables are listed") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/referentietabellen",
                testUser = BEHEERDER_1,
            )

            then(
                """the provisioned default reference tables are returned"""
            ) {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                with(responseBody) {
                    shouldEqualJsonIgnoringExtraneousFields(
                        """
                        [
                            {
                                "valuesCount": 5,
                                "code": "$REFERENCE_TABLE_ADVIES_CODE", 
                                "name": "$REFERENCE_TABLE_ADVIES_NAME", 
                                "systemTable": true
                            },
                            {
                                "valuesCount": 0,
                                "code": "$REFERENCE_TABLE_AFZENDER_CODE", 
                                "name": "$REFERENCE_TABLE_AFZENDER_NAME", 
                                "systemTable": true
                            },
                            {
                                "valuesCount": 15,
                                "code": "$REFERENCE_TABLE_BRP_DOELBINDING_RAADPLEEG_WAARDE_CODE",
                                "name": "$REFERENCE_TABLE_BRP_DOELBINDING_RAADPLEEG_WAARDE_NAAM",
                                "systemTable": true
                            },
                            {
                                "valuesCount": 4,
                                "code": "$REFERENCE_TABLE_BRP_DOELBINDING_ZOEK_WAARDE_CODE",
                                "name": "$REFERENCE_TABLE_BRP_DOELBINDING_ZOEK_WAARDE_NAAM",
                                "systemTable": true
                            },
                            {
                                "valuesCount": 1,
                                "code": "$REFERENCE_TABLE_BRP_VERWERKINGSREGISTER_WAARDE_CODE",
                                "name": "$REFERENCE_TABLE_BRP_VERWERKINGSREGISTER_WAARDE_NAAM",
                                "systemTable": true
                            },
                            {
                                "valuesCount": 8,
                                "code": "$REFERENCE_TABLE_COMMUNICATIEKANAAL_CODE", 
                                "name": "$REFERENCE_TABLE_COMMUNICATIEKANAAL_NAME", 
                                "systemTable": true
                            },                
                            {
                                "valuesCount": 0,
                                "code": "$REFERENCE_TABLE_SERVER_ERROR_ERROR_PAGINA_TEKST_CODE", 
                                "name": "$REFERENCE_TABLE_SERVER_ERROR_ERROR_PAGINA_TEKST_NAME", 
                                "systemTable": true
                            }
                        ]
                        """.trimIndent()
                    )
                }
                with(JSONArray(responseBody)) {
                    communicationChannelReferenceTableId = getJSONObject(5).getInt("id")
                    serverErrorTextErrorReferenceTableId = getJSONObject(6).getInt("id")
                }
            }
        }

        `when`("the get afzenders endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/referentietabellen/afzender",
                testUser = BEHEERDER_1,
            )

            then(
                """an empty list should be returned since we do not provision any default afzenders"""
            ) {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                JSONArray(responseBody).length() shouldBe 0
            }
        }

        `when`("the communication channels reference table is retrieved") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/referentietabellen/$communicationChannelReferenceTableId",
                testUser = BEHEERDER_1,
            )

            then(
                """the provisioned default communicatiekanalen are returned including 'E-formulier'"""
            ) {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                with(JSONObject(responseBody).toString()) {
                    shouldEqualJsonIgnoringExtraneousFields(
                        """
                        {
                            "code": "$REFERENCE_TABLE_COMMUNICATIEKANAAL_CODE",
                            "name": "$REFERENCE_TABLE_COMMUNICATIEKANAAL_NAME",
                            "systemTable": true,
                            "valuesCount": 8,
                            "values": [
                                {"name": "Balie", "systemValue": false},
                                {"name": "E-formulier", "systemValue": true},
                                {"name": "E-mail", "systemValue": false},
                                {"name": "Intern", "systemValue": false},
                                {"name": "Internet", "systemValue": false},
                                {"name": "Medewerkersportaal", "systemValue": false},
                                {"name": "Post", "systemValue": false},
                                {"name": "Telefoon", "systemValue": false}
                            ]
                        }
                        """.trimIndent()
                    )
                    shouldContainJsonKey("id")
                }
            }
        }

        `when`("the get communication channels endpoint is called with 'true' as parameter") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/referentietabellen/communicatiekanaal/true",
                testUser = RAADPLEGER_1
            )

            then(
                """the provisioned default communicatiekanalen are returned including 'E-formulier'"""
            ) {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
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

        `when`("the get server error texts endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/referentietabellen/server-error-text",
                testUser = RAADPLEGER_1
            )

            then(
                """an empty list should be returned since we do not provision any default server error texts"""
            ) {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                JSONArray(responseBody).length() shouldBe 0
            }
        }

        `when`("a reference value is added to the server error texts reference table and the name is updated") {
            val response = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/referentietabellen/$serverErrorTextErrorReferenceTableId",
                requestBodyAsString = """
                    {       
                        "name": "Updated server error error pagina tekst",
                        "values":[ {"name":"fakeServerErrorErrorPageText"} ]
                    }
                """.trimIndent(),
                testUser = BEHEERDER_1,
            )

            then("the response should be 'ok'") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                with(JSONObject(responseBody).toString()) {
                    shouldEqualJsonIgnoringExtraneousFields(
                        """
                        {
                            "code": "$REFERENCE_TABLE_SERVER_ERROR_ERROR_PAGINA_TEKST_CODE",
                            "name": "Updated server error error pagina tekst",
                            "systemTable": true,
                            "valuesCount": 1,
                            "values": [ {"name": "fakeServerErrorErrorPageText", "systemValue": false} ]
                        }
                        """.trimIndent()
                    )
                    shouldContainJsonKey("id")
                }
            }
        }

        `when`("the get server error texts endpoint is called again") {
            val response = itestHttpClient.performGetRequest(
                url = "$ZAC_API_URI/referentietabellen/server-error-text",
                testUser = BEHEERDER_1,
            )

            then(
                """the provisioned default server error texts are returned including the added 'test'"""
            ) {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                with(JSONArray(responseBody)) {
                    length() shouldBe 1
                    shouldContainInOrder(listOf("fakeServerErrorErrorPageText"))
                }
            }
        }

        `when`("a new reference table is added with a code and naam exceeding the maximum length") {
            val response = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/referentietabellen",
                requestBodyAsString = """
                    {
                    "code": "${"a".repeat(257)}",
                    "name": "${"a".repeat(257)}",
                    "values": []
                    }
                """.trimIndent(),
                testUser = BEHEERDER_1,
            )

            then("the response should be 'bad request'") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_BAD_REQUEST
            }
        }

        `when`("a new reference table is added with a value name exceeding the maximum length") {
            val response = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/referentietabellen",
                requestBodyAsString = """
                    {
                    "code": "fakeReferenceTableCode2",
                    "name": "fakeReferenceTableName2",
                    "values": [{"name": "${"a".repeat(1001)}"}]
                    }
                """.trimIndent(),
                testUser = BEHEERDER_1,
            )

            then("the response should be 'bad request'") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_BAD_REQUEST
            }
        }

        `when`("a new reference table is added") {
            val referenceTableCode = "fakeReferenceTableCode1"
            val referenceTableName = "fakeReferenceTableName1"
            val response = itestHttpClient.performJSONPostRequest(
                url = "$ZAC_API_URI/referentietabellen",
                requestBodyAsString = """
                    {       
                    "code": "$referenceTableCode",
                    "name": "$referenceTableName",
                    "values": [{"name":"fakeReferenceTableValue1"}, {"name":"fakeReferenceTableValue2"}]
                    }
                """.trimIndent(),
                testUser = BEHEERDER_1,
            )

            then("the response should be 'ok' and should return the created reference table with code in uppercase") {
                val responseBody = response.bodyAsString
                logger.info { "Response: $responseBody" }
                response.code shouldBe HTTP_OK
                with(JSONObject(responseBody).toString()) {
                    shouldEqualJsonIgnoringExtraneousFields(
                        """
                        {
                            "code": "${referenceTableCode.uppercase()}",
                            "name": "$referenceTableName",
                            "systemTable": false,
                            "valuesCount": 2,
                            "values": [
                                {"name": "fakeReferenceTableValue1", "systemValue": false},
                                {"name": "fakeReferenceTableValue2", "systemValue": false}
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
