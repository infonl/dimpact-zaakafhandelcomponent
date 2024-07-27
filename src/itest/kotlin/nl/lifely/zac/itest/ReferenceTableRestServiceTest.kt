/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_ADVIES_CODE
import nl.lifely.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_ADVIES_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_AFZENDER_CODE
import nl.lifely.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_AFZENDER_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_COMMUNICATIEKANAAL_CODE
import nl.lifely.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_COMMUNICATIEKANAAL_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_DOMEIN_CODE
import nl.lifely.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_DOMEIN_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_SERVER_ERROR_ERROR_PAGINA_TEKST_CODE
import nl.lifely.zac.itest.config.ItestConfiguration.REFERENCE_TABLE_SERVER_ERROR_ERROR_PAGINA_TEKST_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.json.JSONArray

@Suppress("MagicNumber")
class ReferenceTableRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    var serverErrorTextErrorPageReferenceTableId: Int = 0

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
                with(JSONArray(responseBody)) {
                    length() shouldBe 5
                    with(getJSONObject(0).toString()) {
                        shouldContainJsonKeyValue("code", REFERENCE_TABLE_ADVIES_CODE)
                        shouldContainJsonKeyValue("naam", REFERENCE_TABLE_ADVIES_NAME)
                        shouldContainJsonKeyValue("systeem", true)
                        shouldContainJsonKeyValue("aantalWaarden", 5)
                        shouldContainJsonKey("id")
                    }
                    with(getJSONObject(1).toString()) {
                        shouldContainJsonKeyValue("code", REFERENCE_TABLE_AFZENDER_CODE)
                        shouldContainJsonKeyValue("naam", REFERENCE_TABLE_AFZENDER_NAME)
                        shouldContainJsonKeyValue("systeem", true)
                        shouldContainJsonKeyValue("aantalWaarden", 0)
                        shouldContainJsonKey("id")
                    }
                    with(getJSONObject(2).toString()) {
                        shouldContainJsonKeyValue("code", REFERENCE_TABLE_COMMUNICATIEKANAAL_CODE)
                        shouldContainJsonKeyValue("naam", REFERENCE_TABLE_COMMUNICATIEKANAAL_NAME)
                        shouldContainJsonKeyValue("systeem", true)
                        shouldContainJsonKeyValue("aantalWaarden", 8)
                        shouldContainJsonKey("id")
                    }
                    with(getJSONObject(3).toString()) {
                        shouldContainJsonKeyValue("code", REFERENCE_TABLE_DOMEIN_CODE)
                        shouldContainJsonKeyValue("naam", REFERENCE_TABLE_DOMEIN_NAME)
                        shouldContainJsonKeyValue("systeem", true)
                        shouldContainJsonKeyValue("aantalWaarden", 1)
                        shouldContainJsonKey("id")
                    }
                    with(getJSONObject(4).toString()) {
                        shouldContainJsonKeyValue("code", REFERENCE_TABLE_SERVER_ERROR_ERROR_PAGINA_TEKST_CODE)
                        shouldContainJsonKeyValue("naam", REFERENCE_TABLE_SERVER_ERROR_ERROR_PAGINA_TEKST_NAME)
                        shouldContainJsonKeyValue("systeem", true)
                        shouldContainJsonKeyValue("aantalWaarden", 0)
                        shouldContainJsonKey("id")
                    }
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
        When("the get communicatiekanalen endpoint is called with 'true' as parameter") {
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
        When("a reference value is added to the server error texts reference table") {
            val response = itestHttpClient.performPutRequest(
                url = "$ZAC_API_URI/referentietabellen/$serverErrorTextErrorPageReferenceTableId",
                requestBodyAsString = """
                    {       
                    "waarden":[{"naam":"dummyServerErrorErrorPageText"}]
                    }
                """.trimIndent()
            )
            Then("the response should be 'ok'") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
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
    }
})
