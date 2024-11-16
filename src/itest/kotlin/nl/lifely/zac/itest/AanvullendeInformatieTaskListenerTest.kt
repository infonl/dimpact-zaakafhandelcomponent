/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldBeJsonObject
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.client.ZacClient
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_SEARCH
import nl.lifely.zac.itest.config.ItestConfiguration.ZAC_API_URI
import nl.lifely.zac.itest.config.ItestConfiguration.zaakProductaanvraag1Uuid
import org.json.JSONArray

/**
 * This test assumes a human task plan item (=task) has been started for a zaak in a previously run test.
 */
@Order(TEST_SPEC_ORDER_AFTER_SEARCH)
class AanvullendeInformatieTaskListenerTest : BehaviorSpec({
    val logger = KotlinLogging.logger {}
    val itestHttpClient = ItestHttpClient()
    val zacClient = ZacClient()

    Given("A zaak with one Aanvullende informatie task") {
        val tasksResponse = itestHttpClient.performGetRequest(
            "$ZAC_API_URI/taken/zaak/$zaakProductaanvraag1Uuid"
        )
        val responseBody = tasksResponse.body!!.string()
        logger.info { "Response: $responseBody" }
        tasksResponse.isSuccessful shouldBe true

        val taskArray = JSONArray(responseBody)

        When("the last additional information task is closed") {
            val taskObject = taskArray.getJSONObject(0)
            taskObject.put("toelichting", "completed")
            taskObject.put("status", "AFGEROND")

            val completeTaskResponse = itestHttpClient.performPatchRequest(
                url = "$ZAC_API_URI/taken/complete",
                requestBodyAsString = taskObject.toString()
            )

            Then("the taken toelichting and status are updated") {
                val responseBody = completeTaskResponse.body!!.string()
                logger.info { "Response: $responseBody" }
                completeTaskResponse.isSuccessful shouldBe true
                responseBody.shouldBeJsonObject()
                responseBody.shouldContainJsonKeyValue("toelichting", "completed")
                responseBody.shouldContainJsonKeyValue("status", "AFGEROND")
            }

            And("the zaak status is set back to `Intake`") {
                val response = zacClient.retrieveZaak(zaakProductaanvraag1Uuid)
                val responseBody = response.body!!.string()
                logger.info { "Response: $responseBody" }
                response.isSuccessful shouldBe true

                responseBody.shouldContainJsonKeyValue("$.status.naam", "Intake")
            }
        }
    }
})
