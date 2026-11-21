/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldBeJsonObject
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.info.zac.itest.client.ItestHttpClient
import nl.info.zac.itest.client.ZacClient
import nl.info.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_ZAAK_UPDATED
import nl.info.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.info.zac.itest.config.ItestConfiguration.zaakProductaanvraag1Uuid
import org.json.JSONArray

/**
 * This test assumes a human task plan item (=task) has been started for a zaak in a previously run test.
 */
@Order(TEST_SPEC_ORDER_AFTER_ZAAK_UPDATED)
class TaskRestServiceCompleteTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()

    Given("A zaak has been created") {
        lateinit var taskArray: JSONArray

        When("the get tasks for a zaak endpoint is called") {
            val response = itestHttpClient.performGetRequest(
                "$ZAC_API_URI/taken/zaak/$zaakProductaanvraag1Uuid"
            )

            Then("the list with tasks for this zaak is returned") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true

                taskArray = JSONArray(responseBody)
            }
        }

        When("first task is completed") {
            val taskObject = taskArray.getJSONObject(0)
            taskObject.put("toelichting", "completed")
            taskObject.put("status", "AFGEROND")

            val response = itestHttpClient.performPatchRequest(
                url = "$ZAC_API_URI/taken/complete",
                requestBodyAsString = taskObject.toString()
            )

            Then("the taken toelichting and status are updated") {
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true
                responseBody.shouldBeJsonObject()
                responseBody.shouldContainJsonKeyValue("toelichting", "completed")
                responseBody.shouldContainJsonKeyValue("status", "AFGEROND")
            }

            And("the zaak status remains in `aanvullende informatie`") {
                val response = zacClient.retrieveZaak(zaakProductaanvraag1Uuid)
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true

                responseBody.shouldContainJsonKeyValue("$.status.naam", "Wacht op aanvullende informatie")
            }
        }
    }
})
