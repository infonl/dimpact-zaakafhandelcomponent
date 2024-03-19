/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.lifely.zac.itest

import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.json.shouldBeJsonArray
import io.kotest.assertions.json.shouldBeJsonObject
import io.kotest.assertions.json.shouldContainJsonKey
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.json.shouldNotContainJsonKey
import io.kotest.core.spec.Order
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import nl.lifely.zac.itest.PlanItemsRESTServiceTest.Companion.FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE
import nl.lifely.zac.itest.PlanItemsRESTServiceTest.Companion.HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM
import nl.lifely.zac.itest.client.ItestHttpClient
import nl.lifely.zac.itest.config.ItestConfiguration
import nl.lifely.zac.itest.config.ItestConfiguration.GROUP_A_ID
import nl.lifely.zac.itest.config.ItestConfiguration.GROUP_A_NAME
import nl.lifely.zac.itest.config.ItestConfiguration.TEST_SPEC_ORDER_AFTER_TASK_CREATED
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION
import nl.lifely.zac.itest.config.ItestConfiguration.ZAAK_1_IDENTIFICATION
import org.json.JSONArray
import org.json.JSONObject

lateinit var task1ID: String

/**
 * This test assumes a human task plan item (=task) has been started for a zaak in a previously run test.
 */
@Order(TEST_SPEC_ORDER_AFTER_TASK_CREATED)
class TakenRESTServiceTest : BehaviorSpec() {
    private val logger = KotlinLogging.logger {}
    private val itestHttpClient = ItestHttpClient()

    init {
        Given("A zaak has been created") {
            lateinit var responseBody: String

            When("the get tasks for a zaak endpoint is called") {
                val response = itestHttpClient.performGetRequest(
                    "${ItestConfiguration.ZAC_API_URI}/taken/zaak/$zaak1UUID"
                )
                Then(
                    """the list of taken for this zaak is returned and contains the task 
                    'aanvullende informatie' which was started previously"""
                ) {
                    responseBody = response.body!!.string()
                    logger.info { "Response: $responseBody" }
                    response.isSuccessful shouldBe true
                    responseBody.shouldBeJsonArray()
                    // the zaak is in the intake phase, so there should be only be one human task
                    // plan item: 'aanvullende informatie'
                    JSONArray(responseBody).length() shouldBe 1
                    with(JSONArray(responseBody)[0].toString()) {
                        shouldContainJsonKeyValue("naam", HUMAN_TASK_AANVULLENDE_INFORMATIE_NAAM)
                        shouldContainJsonKeyValue(
                            "formulierDefinitieId",
                            FORMULIER_DEFINITIE_AANVULLENDE_INFORMATIE
                        )
                        shouldContainJsonKeyValue("status", "NIET_TOEGEKEND")
                        shouldContainJsonKeyValue("zaakIdentificatie", ZAAK_1_IDENTIFICATION)
                        shouldContainJsonKeyValue(
                            "zaaktypeOmschrijving",
                            ZAAKTYPE_MELDING_KLEIN_EVENEMENT_DESCRIPTION
                        )
                        shouldContainJsonKeyValue("zaakUuid", zaak1UUID.toString())
                        JSONObject(this,).getJSONObject("groep").apply {
                            getString("id") shouldBe GROUP_A_ID
                            getString("naam") shouldBe GROUP_A_NAME
                        }
                        shouldContainJsonKey("id")
                        shouldNotContainJsonKey("toelichting")
                        task1ID = JSONObject(this).getString("id")
                    }
                }
            }
            When("update task endpoint is called") {
                val taskArray = JSONArray(responseBody)
                val taskObject = taskArray.getJSONObject(0)
                taskObject.put("toelichting", "update")

                val response = itestHttpClient.performPutRequest(
                    url = "${ItestConfiguration.ZAC_API_URI}/taken/taakdata",
                    requestBodyAsString = taskArray.toString()
                )

                Then("the taken toelichting is updated") {
                    responseBody = response.body!!.string()
                    logger.info { "Response: $responseBody" }
                    response.isSuccessful shouldBe true
                    responseBody.shouldBeJsonObject()
                    responseBody.shouldContainJsonKeyValue("toelichting", "update")
                }
            }
        }
    }
}
