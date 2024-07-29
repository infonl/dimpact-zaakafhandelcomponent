/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import org.json.JSONArray

@Suppress("MagicNumber")
class ReferentieTabelRestServiceTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()

    Given("Default referentietabel data is provisioned on startup") {
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
    }
})
